import { useState, type FormEvent } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { Activity, Loader2 } from 'lucide-react';
import { toast } from 'sonner';

import { createUserFromLink } from '@/services/api';
import { GlassCard } from '@/components/GlassCard';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export default function Invite() {
  const [searchParams] = useSearchParams();
  const codeFromUrl = searchParams.get('code') ?? '';

  const [code, setCode] = useState(codeFromUrl);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);

    try {
      await createUserFromLink(email, password, code);
      toast.success('Account created! Redirecting to login…');
      setTimeout(() => navigate('/login'), 1500);
    } catch (err: unknown) {
      const message =
        err instanceof Error
          ? err.message
          : 'Could not create account. The invite link may have expired.';
      toast.error(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-mesh flex items-center justify-center p-4">
      <GlassCard className="p-8 w-full max-w-md">
        {/* Brand */}
        <div className="flex items-center justify-center gap-3 mb-8">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-primary/15 border border-primary/25">
            <Activity className="h-4 w-4 text-primary" />
          </div>
          <span className="font-heading text-sm font-bold tracking-wide">
            UPTIME<span className="text-primary">MON</span>
          </span>
        </div>

        <h1 className="font-heading text-2xl font-bold text-center mb-1">
          Join via invite
        </h1>
        <p className="text-sm text-muted-foreground text-center mb-6">
          Use an invite link from your organization admin to create your account.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Only show code field when it wasn't provided in the URL */}
          {!codeFromUrl && (
            <div className="space-y-2">
              <Label htmlFor="code">Invite code</Label>
              <Input
                id="code"
                type="text"
                placeholder="Paste your invite code"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                required
              />
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          <Button type="submit" className="w-full" disabled={loading || !code}>
            {loading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin mr-2" />
                Creating account…
              </>
            ) : (
              'Join organization'
            )}
          </Button>
        </form>

        <div className="mt-6 text-center text-sm text-muted-foreground">
          <p>
            Already have an account?{' '}
            <Link to="/login" className="text-primary hover:underline font-medium">
              Sign in
            </Link>
          </p>
        </div>
      </GlassCard>
    </div>
  );
}
