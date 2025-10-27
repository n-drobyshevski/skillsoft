"use client";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Activity } from "lucide-react";

// Placeholder data
const activities = [
  { id: 1, user: "Alex", action: "updated", competency: "Teamwork", time: "2 hours ago" },
  { id: 2, user: "Sarah", action: "added", competency: "Communication", time: "5 hours ago" },
  { id: 3, user: "John", action: "archived", competency: "Project Management", time: "1 day ago" },
  { id: 4, user: "Emily", action: "updated", competency: "Leadership", time: "2 days ago" },
];

export default function RecentActivityCard() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Activity className="w-5 h-5" />
          Recent Activity
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {activities.map((activity) => (
            <div key={activity.id} className="flex items-start">
              <div className="flex-shrink-0">
                <div className="h-8 w-8 rounded-full bg-muted flex items-center justify-center text-xs">
                  {activity.user.charAt(0)}
                </div>
              </div>
              <div className="ml-3 flex-1">
                <p className="text-sm">
                  <span className="font-medium">{activity.user}</span> {activity.action} the competency{' '}
                  <span className="font-medium">{activity.competency}</span>.
                </p>
                <p className="text-xs text-muted-foreground">{activity.time}</p>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
