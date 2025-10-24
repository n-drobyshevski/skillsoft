import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { ThemeProvider } from './components/theme-provider'
import { Layout } from './components/Layout'
import CompetencyDashboard from './components/CompetencyDashboard'
import CompetenciesPage from './components/CompetenciesPage'
import CompetencyDetailPage from './components/CompetencyDetailPage'
import BehavioralIndicatorsPage from './components/BehavioralIndicatorsPage'
import AssessmentQuestionsPage from './components/AssessmentQuestionsPage'
import './App.css'

function App() {
  return (
    <ThemeProvider defaultTheme="dark" storageKey="skillsoft-ui-theme">
      <Router>
        <Layout>
          <Routes>
            <Route path="/" element={<CompetencyDashboard />} />
            <Route path="/competencies" element={<CompetenciesPage />} />
            <Route path="/competency/:id" element={<CompetencyDetailPage />} />
            <Route path="/behavioral-indicators" element={<BehavioralIndicatorsPage />} />
            <Route path="/assessment-questions" element={<AssessmentQuestionsPage />} />
          </Routes>
        </Layout>
      </Router>
    </ThemeProvider>
  )
}

export default App;
