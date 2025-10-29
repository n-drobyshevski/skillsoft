'use client';

import {
	SidebarInset,
	SidebarProvider,
} from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/app-sidebar";
import { ThemeProvider } from "next-themes";

import { HeaderProvider } from "../context/HeaderContext";
import Header from "../../app/components/Header";

export function LayoutProvider({ children }: { children: React.ReactNode }) {
	return (
		<ThemeProvider
			attribute="class"
			defaultTheme="system"
			enableSystem
			disableTransitionOnChange
		>
			<SidebarProvider defaultOpen={false}>
				<AppSidebar />
				<SidebarInset>
					<header className="flex h-14 md:h-16 shrink-0 items-center gap-2 transition-[width,height] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-12">
						<div className="flex flex-1 items-center gap-2 px-4">
							<SidebarTrigger className="-ml-1" />
							<Separator
								orientation="vertical"
								className="hidden mr-2 data-[orientation=vertical]:h-4 md:block"
							/>
							<Breadcrumb>
								<BreadcrumbList>
									{segments.slice(0, -1).map((segment, index) => (
										<div key={index}>
											<BreadcrumbItem className="hidden md:block">
												<BreadcrumbLink
													href={segments.slice(0, index).join("/")}
												>
													{segment}
												</BreadcrumbLink>
											</BreadcrumbItem>
											{pathname.slice(1, -1).includes("/") && index > 0 ? (
												<BreadcrumbSeparator className="hidden md:block" />
											) : null}
										</div>
									))}

									<BreadcrumbItem>
										<BreadcrumbPage>
											{segments[segments.length - 1].charAt(0).toUpperCase() +
												segments[segments.length - 1].slice(1)}
										</BreadcrumbPage>
									</BreadcrumbItem>
								</BreadcrumbList>
							</Breadcrumb>
						</div>
					</header>

					<div className="flex flex-1 flex-col gap-4 p-4 pt-0">{children}</div>
				</SidebarInset>
			</SidebarProvider>
		</ThemeProvider>
	);
}
