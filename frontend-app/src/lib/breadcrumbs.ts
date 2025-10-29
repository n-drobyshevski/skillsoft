
import { usePathname } from "next/navigation";

export const useBreadcrumbs = () => {
  const pathname = usePathname();
  const segments = pathname.split("/").filter(Boolean);

  const breadcrumbs = segments.map((segment, index) => {
    const href = "/" + segments.slice(0, index + 1).join("/");
    const label = segment.charAt(0).toUpperCase() + segment.slice(1);
    return { href, label };
  });

  let title = "";
  if (breadcrumbs.length > 0) {
    title = breadcrumbs[breadcrumbs.length - 1].label;
  }

  if (pathname.includes("/edit")) {
    title = "Edit";
  }

  return { breadcrumbs, title };
};

