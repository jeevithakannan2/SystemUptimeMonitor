import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useTheme } from 'next-themes';
import { toast } from 'sonner';
import { useAuth } from '@/hooks/useAuth';
import { useSidebar } from '@/hooks/useSidebar';
import { getSubscriptions, updateNotificationPreference } from '@/services/api';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { cn } from '@/lib/utils';
import { motion } from 'framer-motion';
import {
  LayoutGrid,
  Shield,
  Globe,
  PanelLeftClose,
  PanelLeftOpen,
  Sun,
  Moon,
  LogOut,
  Activity,
  Bell,
  BellOff,
} from 'lucide-react';

const COLLAPSED_WIDTH = 64;
const EXPANDED_WIDTH = 220;

export function NavRail() {
  const { expanded, toggle } = useSidebar();
  const { user, isAdmin, logout } = useAuth();
  const { theme, setTheme } = useTheme();
  const location = useLocation();
  const navigate = useNavigate();

  const [emailNotifications, setEmailNotifications] = useState(false);

  useEffect(() => {
    getSubscriptions()
      .then((data) => setEmailNotifications(data.email_notifications))
      .catch(() => {});
  }, []);

  const toggleNotifications = async () => {
    try {
      const newValue = !emailNotifications;
      await updateNotificationPreference(newValue);
      setEmailNotifications(newValue);
      toast.success(newValue ? 'Email notifications enabled' : 'Email notifications disabled');
    } catch {
      toast.error('Failed to update notification preference');
    }
  };

  const initials = user?.email
    ? user.email.split('@')[0].slice(0, 2).toUpperCase()
    : '??';

  const navItems = [
    { to: '/', icon: LayoutGrid, label: 'Dashboard' },
    ...(isAdmin ? [{ to: '/admin', icon: Shield, label: 'Admin' }] : []),
    { to: '/status', icon: Globe, label: 'Status' },
  ];

  const isActive = (path: string) => {
    if (path === '/') return location.pathname === '/';
    return location.pathname.startsWith(path);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <motion.aside
      className="fixed left-0 top-0 z-50 flex h-screen flex-col glass-subtle overflow-hidden"
      initial={false}
      animate={{ width: expanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH }}
      transition={{ type: 'spring', stiffness: 400, damping: 30 }}
    >
      {/* Logo + Toggle */}
      <div className="flex h-16 items-center shrink-0 px-2">
        <Link to="/" className="flex items-center gap-3 px-2 min-w-0">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-primary/15 border border-primary/25">
            <Activity className="h-4 w-4 text-primary" />
          </div>
          <motion.span
            className="font-heading text-sm font-bold tracking-wide whitespace-nowrap overflow-hidden"
            animate={{ opacity: expanded ? 1 : 0, width: expanded ? 'auto' : 0 }}
            transition={{ duration: 0.15 }}
          >
            UPTIME<span className="text-primary">MON</span>
          </motion.span>
        </Link>

        <motion.button
          onClick={toggle}
          className="ml-auto flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-muted-foreground hover:text-foreground hover:bg-accent/60 transition-colors cursor-pointer border-none bg-transparent"
          animate={{ opacity: expanded ? 1 : 0, width: expanded ? 28 : 0 }}
          transition={{ duration: 0.15 }}
        >
          {expanded ? <PanelLeftClose className="h-4 w-4" /> : <PanelLeftOpen className="h-4 w-4" />}
        </motion.button>
      </div>

      {!expanded && (
        <button
          onClick={toggle}
          className="flex h-8 w-8 mx-auto mb-1 shrink-0 items-center justify-center rounded-lg text-muted-foreground hover:text-foreground hover:bg-accent/60 transition-colors cursor-pointer border-none bg-transparent"
        >
          <PanelLeftOpen className="h-4 w-4" />
        </button>
      )}

      {/* Main nav */}
      <nav className="flex flex-col gap-1 px-2 mt-1 flex-1">
        {navItems.map((item) => (
          <Link
            key={item.to}
            to={item.to}
            className={cn(
              'group relative flex items-center rounded-xl py-2.5 text-sm font-medium transition-all',
              expanded ? 'gap-3 px-3' : 'justify-center px-0',
              isActive(item.to)
                ? 'bg-primary/12 text-primary'
                : 'text-muted-foreground hover:text-foreground hover:bg-accent/60',
            )}
          >
            {isActive(item.to) && (
              <motion.div
                layoutId="nav-active"
                className="absolute left-0 top-1/2 -translate-y-1/2 h-5 w-[3px] rounded-r-full bg-primary"
                transition={{ type: 'spring', stiffness: 400, damping: 30 }}
              />
            )}
            <item.icon className="h-[18px] w-[18px] shrink-0" />
            {expanded && (
              <motion.span
                className="whitespace-nowrap overflow-hidden"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ duration: 0.15, delay: 0.05 }}
              >
                {item.label}
              </motion.span>
            )}
          </Link>
        ))}
      </nav>

      {/* Bottom section */}
      <div className="flex flex-col gap-1 px-2 pb-4">
        <button
          onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
          className={cn(
            'group relative flex items-center rounded-xl py-2.5 text-sm font-medium transition-all cursor-pointer border-none bg-transparent',
            expanded ? 'gap-3 px-3' : 'justify-center px-0',
            'text-muted-foreground hover:text-foreground hover:bg-accent/60',
          )}
        >
          <span className="relative h-[18px] w-[18px] shrink-0">
            <Sun className="absolute inset-0 h-[18px] w-[18px] rotate-0 scale-100 transition-transform dark:-rotate-90 dark:scale-0" />
            <Moon className="absolute inset-0 h-[18px] w-[18px] rotate-90 scale-0 transition-transform dark:rotate-0 dark:scale-100" />
          </span>
          {expanded && (
            <motion.span className="whitespace-nowrap overflow-hidden" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.15, delay: 0.05 }}>
              Theme
            </motion.span>
          )}
        </button>

        <button
          onClick={toggleNotifications}
          className={cn(
            'group relative flex items-center rounded-xl py-2.5 text-sm font-medium transition-all cursor-pointer border-none bg-transparent',
            expanded ? 'gap-3 px-3' : 'justify-center px-0',
            emailNotifications
              ? 'text-primary hover:bg-accent/60'
              : 'text-muted-foreground hover:text-foreground hover:bg-accent/60',
          )}
        >
          {emailNotifications
            ? <Bell className="h-[18px] w-[18px] shrink-0" />
            : <BellOff className="h-[18px] w-[18px] shrink-0" />
          }
          {expanded && (
            <motion.span className="whitespace-nowrap overflow-hidden" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.15, delay: 0.05 }}>
              Notifications
            </motion.span>
          )}
        </button>

        <button
          onClick={handleLogout}
          className={cn(
            'group relative flex items-center rounded-xl py-2.5 text-sm font-medium transition-all cursor-pointer border-none bg-transparent',
            expanded ? 'gap-3 px-3' : 'justify-center px-0',
            'text-muted-foreground hover:text-foreground hover:bg-accent/60',
          )}
        >
          <LogOut className="h-[18px] w-[18px] shrink-0" />
          {expanded && (
            <motion.span className="whitespace-nowrap overflow-hidden" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.15, delay: 0.05 }}>
              Logout
            </motion.span>
          )}
        </button>

        <div
          className={cn(
            'flex items-center rounded-xl py-2.5',
            expanded ? 'gap-3 px-3' : 'justify-center px-0',
          )}
        >
          <Avatar className="h-[18px] w-[18px] shrink-0">
            <AvatarFallback className="bg-primary/12 text-primary text-[0.5rem] font-semibold rounded-md">
              {initials}
            </AvatarFallback>
          </Avatar>
          {expanded && (
            <motion.span
              className="text-sm font-medium text-muted-foreground whitespace-nowrap overflow-hidden"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ duration: 0.15, delay: 0.05 }}
            >
              {user?.email ?? 'User'}
            </motion.span>
          )}
        </div>
      </div>
    </motion.aside>
  );
}
