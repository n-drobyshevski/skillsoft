"use client";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardTitle } from "@/components/ui/card";
import React, { useState } from "react";

export default function ErrorCard({
    error,
    callback,
}: {
    error: string;
    callback: () => void;
}) {
    return <div className="container mx-auto px-4 py-8 flex items-center justify-center min-h-[60vh]">
				<Card className="max-w-md border-destructive/50">
					<CardContent className="p-8 text-center">
						<div className="text-4xl mb-4">⚠️</div>
						<CardTitle className="text-destructive mb-2">
							Error Loading Dashboard
						</CardTitle>
						<CardDescription className="mb-6">{error}</CardDescription>
						<Button
							onClick={callback}
							variant="destructive"
							className="w-full"
						>
							Try Again
						</Button>
					</CardContent>
				</Card>
			</div>;     }