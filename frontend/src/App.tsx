import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { ThemeProvider } from 'next-themes';
import { AuthProvider, useAuth } from '@/hooks/useAuth';
import { SidebarProvider, useSidebar } from '@/hooks/useSidebar';
import { TooltipProvider } from '@/components/ui/tooltip';
import { Toaster } from '@/components/ui/sonner';
import { NavRail } from '@/components/NavRail';
import Login from '@/pages/Login';
import Register from '@/pages/Register';
import Invite from '@/pages/Invite';
import Dashboard from '@/pages/Dashboard';
import Admin from '@/pages/Admin';
import { Status } from '@/pages/Status';

function AuthLayout() {
  const { isAuthenticated, isOperator } = useAuth();
  const { expanded } = useSidebar();

  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (!isOperator) return <Navigate to="/status" replace />;

  return (
    <div className="flex h-screen overflow-hidden">
      <NavRail />
      <main
        className="flex-1 overflow-y-auto transition-[margin-left] duration-300 ease-out"
        style={{ marginLeft: expanded ? 220 : 64 }}
      >
        <Outlet />
      </main>
    </div>
  );
}

function AdminRoute() {
  const { isAdmin } = useAuth();
  if (!isAdmin) return <Navigate to="/" replace />;
  return <Outlet />;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/invite" element={<Invite />} />
      <Route path="/status" element={<Status />} />
      <Route element={<AuthLayout />}>
        <Route path="/" element={<Dashboard />} />
        <Route element={<AdminRoute />}>
          <Route path="/admin" element={<Admin />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <ThemeProvider attribute="class" defaultTheme="light" disableTransitionOnChange>
      <BrowserRouter>
        <AuthProvider>
          <SidebarProvider>
            <TooltipProvider>
              <div>
                <AppRoutes />
                <Toaster theme="system" toastOptions={{ className: 'glass text-card-foreground' }} />
              </div>
            </TooltipProvider>
          </SidebarProvider>
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  );
}
