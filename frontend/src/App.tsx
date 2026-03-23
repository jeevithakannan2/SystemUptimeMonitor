import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { ThemeProvider } from 'next-themes';
import { AuthProvider, useAuth } from '@/hooks/useAuth';
import { SidebarProvider, useSidebar } from '@/hooks/useSidebar';
import { TooltipProvider } from '@/components/ui/tooltip';
import { Toaster } from '@/components/ui/sonner';
import { NavRail } from '@/components/NavRail';

const Login = lazy(() => import('@/pages/Login'));
const Register = lazy(() => import('@/pages/Register'));
const Invite = lazy(() => import('@/pages/Invite'));
const Dashboard = lazy(() => import('@/pages/Dashboard'));
const Admin = lazy(() => import('@/pages/Admin'));
const Status = lazy(() => import('@/pages/Status').then(m => ({ default: m.Status })));

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
    <Suspense fallback={<div className="flex h-screen items-center justify-center"><div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" /></div>}>
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
    </Suspense>
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
