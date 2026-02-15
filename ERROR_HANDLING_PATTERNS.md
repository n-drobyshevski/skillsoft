# Error Handling Patterns for Next.js 16 & React 19

A comprehensive guide to implementing user-friendly error handling and recovery patterns in the SkillSoft application.

## Table of Contents
1. [Next.js 16 Error Handling Overview](#nextjs-16-error-handling-overview)
2. [Error Boundary Patterns](#error-boundary-patterns)
3. [API Error Handling](#api-error-handling)
4. [Form Validation Error Display](#form-validation-error-display)
5. [User-Friendly Error Messages](#user-friendly-error-messages)
6. [Error Recovery Patterns](#error-recovery-patterns)
7. [Implementation Examples for SkillSoft](#implementation-examples-for-skillsoft)

---

## Next.js 16 Error Handling Overview

Next.js 16 divides errors into two main categories:

### 1. **Expected Errors** (Explicit Handling)
- Validation errors
- API request failures
- Missing resources
- Business logic constraints

**Handling Strategy**: Return errors as values, not exceptions

### 2. **Uncaught Exceptions** (Error Boundaries)
- Rendering errors
- Unexpected runtime failures
- Component lifecycle errors

**Handling Strategy**: Use `error.js` and `global-error.js` files

---

## Error Boundary Patterns

### Route-Level Error Boundaries (`error.js`)

**Location**: Place at the segment level where you want error isolation

```typescript
// app/dashboard/error.tsx
'use client'

import { useEffect } from 'react'

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  useEffect(() => {
    // Log error to monitoring service
    console.error('Dashboard Error:', error)
  }, [error])

  return (
    <div className="flex flex-col items-center justify-center min-h-screen gap-4">
      <h2 className="text-2xl font-bold text-red-600">Something went wrong!</h2>
      <p className="text-gray-600">Error ID: {error.digest}</p>
      <button
        onClick={() => reset()}
        className="px-4 py-2 bg-emerald-600 text-white rounded hover:bg-emerald-700"
      >
        Try again
      </button>
    </div>
  )
}
```

### Global Error Handling (`global-error.tsx`)

**Location**: Root app directory for application-wide errors

```typescript
// app/global-error.tsx
'use client'

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  return (
    <html>
      <body>
        <div className="flex flex-col items-center justify-center min-h-screen gap-4 p-4">
          <h1 className="text-3xl font-bold text-red-600">Application Error</h1>
          <p className="text-gray-600">Something went wrong. Please try again.</p>
          <p className="text-sm text-gray-500">Error ID: {error.digest}</p>
          <button
            onClick={() => reset()}
            className="px-4 py-2 bg-emerald-600 text-white rounded hover:bg-emerald-700"
          >
            Reload Application
          </button>
        </div>
      </body>
    </html>
  )
}
```

### Component-Level Error Boundaries

Isolate failures to specific components to prevent cascading failures:

```typescript
// app/competencies/error.tsx
'use client'

export default function CompetenciesError({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  return (
    <div className="p-6 border border-red-200 rounded-lg bg-red-50">
      <h3 className="font-semibold text-red-900">Failed to Load Competencies</h3>
      <p className="text-sm text-red-700 mt-2">{error.message}</p>
      <button
        onClick={() => reset()}
        className="mt-4 px-3 py-1 text-sm bg-red-600 text-white rounded hover:bg-red-700"
      >
        Retry
      </button>
    </div>
  )
}
```

---

## API Error Handling

### Server Actions with Error Returns

**Pattern**: Return error state instead of throwing exceptions

```typescript
// src/app/actions.ts
'use server'

export async function createTestSession(
  competencyIds: string[],
  templateId: string
) {
  try {
    const response = await fetch(
      `${process.env.NEXT_PUBLIC_API_URL}/api/v1/tests/sessions`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getSessionToken()}`,
        },
        body: JSON.stringify({
          competencyIds,
          templateId,
        }),
      }
    )

    if (!response.ok) {
      const error = await response.json()
      return {
        success: false,
        error: {
          message: error.message || 'Failed to start test session',
          code: error.code,
          details: error.details,
          category: error.category,
        },
      }
    }

    const data = await response.json()
    return {
      success: true,
      data,
    }
  } catch (error) {
    return {
      success: false,
      error: {
        message: 'Network error. Please check your connection.',
        code: 'NETWORK_ERROR',
      },
    }
  }
}
```

### Client-Side API Error Handling

```typescript
// src/services/test-session.ts
import { AxiosError } from 'axios'
import api from './api'

export interface TestSessionError {
  message: string
  code: string
  category: 'VALIDATION' | 'NETWORK' | 'SERVER' | 'UNKNOWN'
  details?: Record<string, any>
  correlationId?: string
}

export async function startTestSession(
  competencyIds: string[],
  templateId: string
): Promise<{ success: boolean; error?: TestSessionError; data?: any }> {
  try {
    const response = await api.post('/api/v1/tests/sessions', {
      competencyIds,
      templateId,
    })
    return { success: true, data: response.data }
  } catch (error) {
    const axiosError = error as AxiosError<any>
    const errorData = axiosError.response?.data

    // Categorize error
    let category: TestSessionError['category'] = 'UNKNOWN'
    if (axiosError.code === 'ERR_NETWORK') {
      category = 'NETWORK'
    } else if (axiosError.response?.status === 400) {
      category = 'VALIDATION'
    } else if (axiosError.response?.status && axiosError.response.status >= 500) {
      category = 'SERVER'
    }

    return {
      success: false,
      error: {
        message: errorData?.message || 'An error occurred',
        code: errorData?.code || 'UNKNOWN_ERROR',
        category,
        details: errorData?.details,
        correlationId: errorData?.correlationId,
      },
    }
  }
}
```

### Specific Error Cases

```typescript
// Example: Handling "No Questions Available" error
export async function handleTestSessionError(
  error: TestSessionError
): Promise<{
  userMessage: string
  suggestedAction: 'RETRY' | 'REVIEW_SETUP' | 'CONTACT_SUPPORT'
  recoveryPath?: string
}> {
  switch (error.code) {
    case 'NO_QUESTIONS_AVAILABLE':
      return {
        userMessage:
          'No assessment questions are available for the selected competencies. ' +
          'Please ensure all competencies have active behavioral indicators with questions.',
        suggestedAction: 'REVIEW_SETUP',
        recoveryPath: '/admin/competencies',
      }

    case 'INVALID_COMPETENCY':
      return {
        userMessage: 'One or more selected competencies are invalid or archived.',
        suggestedAction: 'REVIEW_SETUP',
        recoveryPath: '/admin/competencies',
      }

    case 'TEMPLATE_NOT_FOUND':
      return {
        userMessage: 'The assessment template could not be found.',
        suggestedAction: 'RETRY',
      }

    case 'NETWORK_ERROR':
      return {
        userMessage:
          'Network error. Please check your connection and try again.',
        suggestedAction: 'RETRY',
      }

    case 'SERVER_ERROR':
      return {
        userMessage:
          'Server error. Please try again later or contact support.',
        suggestedAction: 'CONTACT_SUPPORT',
      }

    default:
      return {
        userMessage: error.message,
        suggestedAction: 'RETRY',
      }
  }
}
```

---

## Form Validation Error Display

### React Hook Form + Zod Pattern

```typescript
// src/components/forms/TestSessionForm.tsx
'use client'

import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useActionState } from 'react'
import { createTestSession } from '@/src/app/actions'

const TestSessionSchema = z.object({
  competencyIds: z
    .array(z.string().uuid())
    .min(1, 'Select at least one competency')
    .max(10, 'Maximum 10 competencies allowed'),
  templateId: z.string().uuid('Invalid template selected'),
})

type TestSessionFormData = z.infer<typeof TestSessionSchema>

export function TestSessionForm() {
  const [state, formAction, isPending] = useActionState(
    createTestSession,
    null
  )

  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm<TestSessionFormData>({
    resolver: zodResolver(TestSessionSchema),
  })

  return (
    <form onSubmit={handleSubmit(formAction)} className="space-y-6">
      {/* Competency Selection */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Select Competencies *
        </label>
        <select
          {...register('competencyIds')}
          multiple
          className={`w-full px-3 py-2 border rounded-lg ${
            errors.competencyIds
              ? 'border-red-500 bg-red-50'
              : 'border-gray-300'
          }`}
        >
          <option value="">-- Select competencies --</option>
          {/* Options */}
        </select>
        {errors.competencyIds && (
          <p className="mt-1 text-sm text-red-600" role="alert">
            {errors.competencyIds.message}
          </p>
        )}
      </div>

      {/* Template Selection */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Select Template *
        </label>
        <select
          {...register('templateId')}
          className={`w-full px-3 py-2 border rounded-lg ${
            errors.templateId ? 'border-red-500 bg-red-50' : 'border-gray-300'
          }`}
        >
          <option value="">-- Select template --</option>
          {/* Options */}
        </select>
        {errors.templateId && (
          <p className="mt-1 text-sm text-red-600" role="alert">
            {errors.templateId.message}
          </p>
        )}
      </div>

      {/* Server-Side Validation Error */}
      {state?.error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
          <h4 className="font-semibold text-red-900">Unable to Start Assessment</h4>
          <p className="text-sm text-red-700 mt-2">{state.error.message}</p>
          {state.error.details && (
            <details className="mt-3 text-xs text-red-600">
              <summary className="cursor-pointer font-medium">
                Technical details
              </summary>
              <pre className="mt-2 bg-red-100 p-2 rounded overflow-auto">
                {JSON.stringify(state.error.details, null, 2)}
              </pre>
            </details>
          )}
        </div>
      )}

      <button
        type="submit"
        disabled={isPending}
        className="w-full px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 disabled:bg-gray-400"
      >
        {isPending ? 'Starting Assessment...' : 'Start Assessment'}
      </button>
    </form>
  )
}
```

### Field-Level Validation with Visual Feedback

```typescript
// src/components/forms/FormField.tsx
interface FormFieldProps {
  label: string
  error?: string
  required?: boolean
  children: React.ReactNode
  hint?: string
}

export function FormField({
  label,
  error,
  required,
  children,
  hint,
}: FormFieldProps) {
  return (
    <div className="space-y-1">
      <label className="block text-sm font-medium text-gray-700">
        {label}
        {required && <span className="text-red-500 ml-1">*</span>}
      </label>

      <div className="relative">
        {children}
        {error && (
          <div className="absolute right-2 top-2 text-red-500">
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M18.101 12.93..." />
            </svg>
          </div>
        )}
      </div>

      {error && (
        <p className="text-sm text-red-600 flex items-center gap-1" role="alert">
          <span className="font-medium">Error:</span> {error}
        </p>
      )}

      {hint && !error && (
        <p className="text-xs text-gray-500">{hint}</p>
      )}
    </div>
  )
}
```

---

## User-Friendly Error Messages

### Message Design Principles

1. **Be Specific**: Explain what went wrong
2. **Be Actionable**: Tell users how to fix it
3. **Be Humble**: Don't blame the user
4. **Be Accessible**: Support screen readers and high contrast
5. **Avoid Jargon**: Use plain language

### Error Message Examples

```typescript
// BAD
"Invalid input"
"Error 400"
"Database connection failed"

// GOOD
"Please enter a valid email address (example@company.com)"
"The assessment template wasn't found. Select a different template or contact your administrator."
"We're having trouble connecting to our servers. Please check your internet and try again."
```

### Error Alert Component

```typescript
// src/components/feedback/ErrorAlert.tsx
interface ErrorAlertProps {
  title: string
  message: string
  details?: string
  category?: 'VALIDATION' | 'NETWORK' | 'SERVER' | 'UNKNOWN'
  onDismiss?: () => void
  actions?: Array<{
    label: string
    onClick: () => void
    variant?: 'primary' | 'secondary'
  }>
}

export function ErrorAlert({
  title,
  message,
  details,
  category,
  onDismiss,
  actions,
}: ErrorAlertProps) {
  const bgColor = {
    VALIDATION: 'bg-amber-50 border-amber-200',
    NETWORK: 'bg-orange-50 border-orange-200',
    SERVER: 'bg-red-50 border-red-200',
    UNKNOWN: 'bg-gray-50 border-gray-200',
  }[category || 'UNKNOWN']

  const titleColor = {
    VALIDATION: 'text-amber-900',
    NETWORK: 'text-orange-900',
    SERVER: 'text-red-900',
    UNKNOWN: 'text-gray-900',
  }[category || 'UNKNOWN']

  const iconColor = {
    VALIDATION: 'text-amber-500',
    NETWORK: 'text-orange-500',
    SERVER: 'text-red-500',
    UNKNOWN: 'text-gray-500',
  }[category || 'UNKNOWN']

  return (
    <div
      className={`p-4 border rounded-lg ${bgColor}`}
      role="alert"
      aria-live="polite"
    >
      <div className="flex gap-3">
        <div className={`flex-shrink-0 ${iconColor} mt-0.5`}>
          {category === 'VALIDATION' ? (
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M8.257 3.099c.765-1.36..." />
            </svg>
          ) : (
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M18.364 5.364l-14.364..." />
            </svg>
          )}
        </div>

        <div className="flex-1">
          <h4 className={`font-semibold ${titleColor}`}>{title}</h4>
          <p className={`text-sm mt-1 ${titleColor.replace('900', '700')}`}>
            {message}
          </p>

          {details && (
            <details className="mt-3 text-xs">
              <summary className="cursor-pointer font-medium">
                Show technical details
              </summary>
              <pre className="mt-2 bg-black bg-opacity-5 p-2 rounded overflow-auto text-left">
                {details}
              </pre>
            </details>
          )}

          {actions && (
            <div className="flex gap-2 mt-4">
              {actions.map((action) => (
                <button
                  key={action.label}
                  onClick={action.onClick}
                  className={`px-3 py-1 text-sm rounded font-medium ${
                    action.variant === 'primary'
                      ? 'bg-emerald-600 text-white hover:bg-emerald-700'
                      : 'border border-current opacity-70 hover:opacity-100'
                  }`}
                >
                  {action.label}
                </button>
              ))}
            </div>
          )}
        </div>

        {onDismiss && (
          <button
            onClick={onDismiss}
            className="text-gray-400 hover:text-gray-600"
            aria-label="Dismiss error"
          >
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
              <path
                fillRule="evenodd"
                d="M4.293 4.293a1 1 0 011.414 0L10..."
              />
            </svg>
          </button>
        )}
      </div>
    </div>
  )
}
```

---

## Error Recovery Patterns

### Retry Pattern

```typescript
// src/hooks/useRetryableOperation.ts
import { useState, useCallback } from 'react'

export function useRetryableOperation<T>(
  operation: () => Promise<T>,
  options = { maxRetries: 3, delayMs: 1000 }
) {
  const [state, setState] = useState<{
    data?: T
    error?: Error
    isLoading: boolean
    retryCount: number
  }>({
    isLoading: false,
    retryCount: 0,
  })

  const execute = useCallback(async () => {
    setState((s) => ({ ...s, isLoading: true }))

    let lastError: Error | null = null

    for (let i = 0; i <= options.maxRetries; i++) {
      try {
        const data = await operation()
        setState({ data, isLoading: false, error: undefined, retryCount: 0 })
        return data
      } catch (error) {
        lastError = error as Error
        setState((s) => ({ ...s, retryCount: i + 1 }))

        if (i < options.maxRetries) {
          await new Promise((resolve) =>
            setTimeout(resolve, options.delayMs * (i + 1))
          )
        }
      }
    }

    setState({
      error: lastError || new Error('Operation failed'),
      isLoading: false,
      data: undefined,
      retryCount: options.maxRetries + 1,
    })
    throw lastError
  }, [operation, options.maxRetries, options.delayMs])

  const retry = useCallback(() => execute(), [execute])

  return {
    ...state,
    execute,
    retry,
    canRetry: state.retryCount < options.maxRetries,
  }
}
```

### Graceful Degradation

```typescript
// src/components/dashboard/CompetenciesSection.tsx
'use client'

import { useState } from 'react'
import { ErrorAlert } from '@/src/components/feedback/ErrorAlert'

export function CompetenciesSection() {
  const [hasError, setHasError] = useState(false)
  const [error, setError] = useState<string>()

  if (hasError) {
    return (
      <section className="space-y-4">
        <h2 className="text-xl font-bold">Competencies</h2>
        <ErrorAlert
          title="Unable to Load Competencies"
          message={error || 'Please try again later.'}
          onDismiss={() => setHasError(false)}
          actions={[
            { label: 'Retry', onClick: () => window.location.reload() },
            {
              label: 'Go to Dashboard',
              onClick: () => (window.location.href = '/dashboard'),
              variant: 'secondary',
            },
          ]}
        />
        {/* Show partial content - competencies from cache or placeholders */}
        <div className="grid grid-cols-3 gap-4">
          {/* Skeleton or cached data */}
        </div>
      </section>
    )
  }

  return (
    <section>
      {/* Normal content */}
    </section>
  )
}
```

### Dialog-Based Error Recovery

```typescript
// src/components/dialogs/ErrorRecoveryDialog.tsx
'use client'

import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/src/components/ui/dialog'
import { Button } from '@/src/components/ui/button'

interface ErrorRecoveryDialogProps {
  open: boolean
  title: string
  message: string
  recoveryOptions: Array<{
    id: string
    label: string
    action: () => void | Promise<void>
    variant?: 'primary' | 'secondary' | 'danger'
  }>
  onOpenChange?: (open: boolean) => void
}

export function ErrorRecoveryDialog({
  open,
  title,
  message,
  recoveryOptions,
  onOpenChange,
}: ErrorRecoveryDialogProps) {
  const [isProcessing, setIsProcessing] = useState(false)

  const handleAction = async (action: () => void | Promise<void>) => {
    setIsProcessing(true)
    try {
      await action()
      onOpenChange?.(false)
    } finally {
      setIsProcessing(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>
        <p className="text-gray-600">{message}</p>
        <div className="flex gap-2 justify-end mt-6">
          {recoveryOptions.map((option) => (
            <Button
              key={option.id}
              onClick={() => handleAction(option.action)}
              disabled={isProcessing}
              variant={option.variant || 'secondary'}
            >
              {option.label}
            </Button>
          ))}
        </div>
      </DialogContent>
    </Dialog>
  )
}
```

---

## Implementation Examples for SkillSoft

### Example 1: Test Session Start with Error Handling

```typescript
// app/(workspace)/test-templates/_components/StartTestButton.tsx
'use client'

import { useActionState, useState } from 'react'
import { useRouter } from 'next/navigation'
import { startTestSessionAction } from '@/src/app/actions'
import { ErrorAlert } from '@/src/components/feedback/ErrorAlert'
import { ErrorRecoveryDialog } from '@/src/components/dialogs/ErrorRecoveryDialog'
import { Button } from '@/src/components/ui/button'

interface StartTestButtonProps {
  templateId: string
  competencyIds: string[]
}

export function StartTestButton({
  templateId,
  competencyIds,
}: StartTestButtonProps) {
  const router = useRouter()
  const [showRecoveryDialog, setShowRecoveryDialog] = useState(false)
  const [recoveryError, setRecoveryError] = useState<{
    title: string
    message: string
    recoveryPath?: string
  }>()

  const [state, formAction, isPending] = useActionState(
    async () => {
      const result = await startTestSessionAction(templateId, competencyIds)
      if (!result.success && result.error) {
        setRecoveryError({
          title: 'Cannot Start Assessment',
          message: result.error.message,
        })
        setShowRecoveryDialog(true)
      } else if (result.success) {
        router.push(`/test-templates/take/${result.data.sessionId}`)
      }
      return result
    },
    null
  )

  return (
    <>
      <Button
        onClick={() => formAction()}
        disabled={isPending || competencyIds.length === 0}
        className="w-full"
      >
        {isPending ? 'Starting Assessment...' : 'Start Assessment'}
      </Button>

      {state?.error && (
        <ErrorAlert
          title="Setup Issue"
          message={state.error.message}
          category="VALIDATION"
          actions={[
            {
              label: 'Review Competencies',
              onClick: () => router.push('/admin/competencies'),
              variant: 'primary',
            },
            {
              label: 'Try Again',
              onClick: () => formAction(),
              variant: 'secondary',
            },
          ]}
        />
      )}

      <ErrorRecoveryDialog
        open={showRecoveryDialog}
        title={recoveryError?.title || 'Error'}
        message={recoveryError?.message || ''}
        recoveryOptions={[
          {
            id: 'review',
            label: 'Review Competencies',
            action: () => router.push('/admin/competencies'),
            variant: 'secondary',
          },
          {
            id: 'retry',
            label: 'Try Again',
            action: () => formAction(),
            variant: 'primary',
          },
        ]}
        onOpenChange={setShowRecoveryDialog}
      />
    </>
  )
}
```

### Example 2: Competency Loading with Fallback

```typescript
// app/(workspace)/admin/competencies/page.tsx
'use client'

import { Suspense } from 'react'
import { CompetenciesList } from './_components/CompetenciesList'
import { CompetenciesError } from './_components/CompetenciesError'
import { CompetenciesSkeleton } from './_components/CompetenciesSkeleton'

export default function CompetenciesPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Competencies</h1>

      <Suspense fallback={<CompetenciesSkeleton />} key="competencies">
        <CompetenciesList />
      </Suspense>
    </div>
  )
}

// app/(workspace)/admin/competencies/error.tsx
'use client'

import { useEffect } from 'react'

export default function CompetenciesError({
  error,
  reset,
}: {
  error: Error
  reset: () => void
}) {
  useEffect(() => {
    console.error('Competencies page error:', error)
  }, [error])

  return (
    <div className="rounded-lg border border-red-200 bg-red-50 p-6">
      <h2 className="text-lg font-semibold text-red-900">
        Failed to Load Competencies
      </h2>
      <p className="mt-2 text-sm text-red-700">{error.message}</p>
      <button
        onClick={() => reset()}
        className="mt-4 px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
      >
        Try Again
      </button>
    </div>
  )
}
```

### Example 3: Answer Submission with Validation

```typescript
// src/components/test-player/AnswerSubmission.tsx
'use client'

import { useState } from 'react'
import { useActionState } from 'react'
import { submitTestSession } from '@/src/app/actions'
import { ErrorAlert } from '@/src/components/feedback/ErrorAlert'
import { SubmissionProgress } from './SubmissionProgress'

interface AnswerSubmissionProps {
  sessionId: string
  answers: Record<string, any>
  onSuccess: () => void
}

export function AnswerSubmission({
  sessionId,
  answers,
  onSuccess,
}: AnswerSubmissionProps) {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<{
    message: string
    canRetry: boolean
  }>()

  const [state, formAction, isPending] = useActionState(
    async () => {
      setIsSubmitting(true)
      setSubmitError(undefined)

      try {
        const result = await submitTestSession(sessionId, answers)

        if (!result.success) {
          setSubmitError({
            message: result.error?.message || 'Failed to submit assessment',
            canRetry: result.error?.category === 'NETWORK',
          })
          setIsSubmitting(false)
          return result
        }

        onSuccess()
        return result
      } catch (error) {
        setSubmitError({
          message: 'An unexpected error occurred. Please try again.',
          canRetry: true,
        })
        setIsSubmitting(false)
        throw error
      }
    },
    null
  )

  return (
    <>
      {isSubmitting && <SubmissionProgress />}

      {submitError && (
        <ErrorAlert
          title="Submission Failed"
          message={submitError.message}
          category={
            submitError.canRetry
              ? 'NETWORK'
              : 'SERVER'
          }
          actions={
            submitError.canRetry
              ? [
                  {
                    label: 'Retry Submission',
                    onClick: () => formAction(),
                    variant: 'primary',
                  },
                ]
              : [
                  {
                    label: 'Contact Support',
                    onClick: () => {
                      window.location.href = 'mailto:support@skillsoft.com'
                    },
                  },
                ]
          }
        />
      )}

      <button
        onClick={() => formAction()}
        disabled={isPending}
        className="w-full px-4 py-2 bg-emerald-600 text-white rounded hover:bg-emerald-700 disabled:bg-gray-400"
      >
        {isPending ? 'Submitting...' : 'Submit Assessment'}
      </button>
    </>
  )
}
```

---

## Accessibility Considerations

### Error Announcement

```typescript
// Use ARIA live regions for dynamic errors
<div role="alert" aria-live="polite" aria-atomic="true">
  {error && <p>{error.message}</p>}
</div>
```

### Form Field Association

```typescript
// Associate errors with form fields
<input
  id="email"
  aria-invalid={!!errors.email}
  aria-describedby="email-error"
/>
{errors.email && (
  <span id="email-error" role="alert">
    {errors.email.message}
  </span>
)}
```

### Color + Indicators

```typescript
// Don't rely on color alone
<div className="flex items-center gap-2 text-red-600">
  <ExclamationIcon /> {/* Visual indicator */}
  Invalid input
</div>
```

---

## Testing Error Scenarios

### Component Tests

```typescript
import { render, screen } from '@/src/__tests__/utils/render-with-providers'
import { ErrorAlert } from '@/src/components/feedback/ErrorAlert'

describe('ErrorAlert', () => {
  it('should display error message', () => {
    render(
      <ErrorAlert
        title="Test Error"
        message="This is a test error"
        category="VALIDATION"
      />
    )
    expect(screen.getByText('Test Error')).toBeInTheDocument()
    expect(screen.getByText('This is a test error')).toBeInTheDocument()
  })

  it('should call action when button is clicked', async () => {
    const handleAction = vi.fn()
    render(
      <ErrorAlert
        title="Error"
        message="Error message"
        actions={[{ label: 'Retry', onClick: handleAction }]}
      />
    )
    await userEvent.click(screen.getByText('Retry'))
    expect(handleAction).toHaveBeenCalled()
  })
})
```

### Integration Tests

```typescript
describe('Test Session Creation', () => {
  it('should display validation error when no competencies selected', async () => {
    const { getByRole, getByText } = render(
      <TestSessionForm onSuccess={vi.fn()} />
    )

    await userEvent.click(getByRole('button', { name: /start/i }))

    expect(
      getByText(/select at least one competency/i)
    ).toBeInTheDocument()
  })

  it('should display server error when API fails', async () => {
    server.use(
      http.post('/api/v1/tests/sessions', () => {
        return HttpResponse.json(
          {
            message: 'No questions available',
            code: 'NO_QUESTIONS_AVAILABLE',
          },
          { status: 400 }
        )
      })
    )

    const { getByRole, getByText } = render(
      <TestSessionForm onSuccess={vi.fn()} />
    )

    // Fill form and submit...

    expect(
      getByText(/no questions available/i)
    ).toBeInTheDocument()
  })
})
```

---

## Best Practices Summary

1. **Categorize Errors**: Validation, network, server, unknown
2. **Provide Context**: Include error messages, codes, and IDs
3. **Enable Recovery**: Offer clear next steps (retry, review, contact)
4. **Be User-Friendly**: Use plain language and avoid technical jargon
5. **Handle Async**: Use try-catch for API calls, error boundaries for rendering
6. **Test Thoroughly**: Test happy path, error paths, and recovery flows
7. **Log Appropriately**: Use error monitoring for debugging
8. **Accessibility First**: Use ARIA attributes and semantic HTML

---

## References

- [Next.js 16 Error Handling Documentation](https://nextjs.org/docs/app/getting-started/error-handling)
- [Next.js error.js Convention](https://nextjs.org/docs/app/api-reference/file-conventions/error)
- [Next.js Error Handling Patterns Guide](https://betterstack.com/community/guides/scaling-nodejs/error-handling-nextjs/)
- [React Error Boundaries](https://react.dev/reference/react/Component#catching-rendering-errors-with-an-error-boundary)
- [Web Content Accessibility Guidelines (WCAG)](https://www.w3.org/WAI/WCAG21/quickref/)

