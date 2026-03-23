import { useState, useEffect, useCallback } from "react";
import { toast } from "sonner";
import { UserPlus, Trash2, Copy, Clock, Loader2, Link as LinkIcon, Users } from "lucide-react";

import { generateInviteLink, deleteUser, getUsers, updateUserRole } from "@/services/api";
import type { User } from "@/types";
import { GlassCard } from "@/components/GlassCard";
import { PageHeader } from "@/components/PageHeader";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogClose,
  DialogTrigger,
} from "@/components/ui/dialog";

const INVITE_TTL = 30;

export default function Admin() {

  // ── Invite state ──
  const [inviteUrl, setInviteUrl] = useState("");
  const [secondsLeft, setSecondsLeft] = useState(0);
  const [inviteLoading, setInviteLoading] = useState(false);

  // ── Team Members state ──
  const [users, setUsers] = useState<User[]>([]);
  const [usersLoading, setUsersLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<User | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deletingUser, setDeletingUser] = useState(false);

  const fetchUsers = useCallback(async () => {
    setUsersLoading(true);
    try {
      const { users } = await getUsers();
      setUsers(users);
    } catch (err: any) {
      toast.error(err?.message ?? "Failed to load users");
    } finally {
      setUsersLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleRoleChange = useCallback(async (userId: number, newRole: string) => {
    try {
      await updateUserRole(userId, newRole);
      toast.success("Role updated");
      fetchUsers();
    } catch (err: any) {
      toast.error(err?.message ?? "Failed to update role");
    }
  }, [fetchUsers]);

  const handleDeleteTeamMember = useCallback(async () => {
    if (!deleteTarget) return;
    setDeletingUser(true);
    try {
      await deleteUser(deleteTarget.email);
      toast.success(`User ${deleteTarget.email} removed`);
      fetchUsers();
    } catch (err: any) {
      toast.error(err?.message ?? "Failed to remove user");
    } finally {
      setDeletingUser(false);
      setDeleteDialogOpen(false);
      setDeleteTarget(null);
    }
  }, [deleteTarget, fetchUsers]);

  // ── Remove-user state ──
  const [email, setEmail] = useState("");
  const [removeLoading, setRemoveLoading] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);

  // ── Countdown timer ──
  useEffect(() => {
    if (secondsLeft <= 0) return;
    const id = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          clearInterval(id);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(id);
  }, [inviteUrl]); // restart whenever a new link is generated

  const handleInvite = useCallback(
    async (role: string) => {
      setInviteLoading(true);
      try {
        const code = await generateInviteLink(role);
        const url =
          window.location.origin +
          "/invite?code=" +
          encodeURIComponent(code.trim());
        setInviteUrl(url);
        setSecondsLeft(INVITE_TTL);
        toast.success(`${role.charAt(0).toUpperCase() + role.slice(1)} invite link generated`);
      } catch (err: any) {
        toast.error(err?.message ?? "Failed to generate invite link");
      } finally {
        setInviteLoading(false);
      }
    },
    [],
  );

  const copyToClipboard = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(inviteUrl);
      toast.success("Link copied to clipboard");
    } catch {
      toast.error("Failed to copy link");
    }
  }, [inviteUrl]);

  const handleRemoveUser = useCallback(async () => {
    setRemoveLoading(true);
    try {
      await deleteUser(email);
      toast.success(`User ${email} removed`);
      setEmail("");
    } catch (err: any) {
      toast.error(err?.message ?? "Failed to remove user");
    } finally {
      setRemoveLoading(false);
      setDialogOpen(false);
    }
  }, [email]);

  const expired = inviteUrl !== "" && secondsLeft === 0;

  return (
    <div className="mx-auto max-w-screen-xl px-8 py-8 space-y-8">
      <PageHeader title="Admin" description="Manage your organization" />

      {/* ── Invite Users ── */}
      <GlassCard className="p-6 space-y-5">
        <div>
          <h2 className="font-heading text-lg font-semibold">Invite Users</h2>
          <p className="text-sm text-muted-foreground">
            Generate a single-use invite code that expires after 30 seconds.
          </p>
        </div>

        <div className="flex flex-wrap gap-3">
          <Button
            onClick={() => handleInvite("operator")}
            disabled={inviteLoading}
          >
            {inviteLoading ? (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            ) : (
              <UserPlus className="mr-2 h-4 w-4" />
            )}
            Invite Operator
          </Button>

          <Button
            variant="outline"
            onClick={() => handleInvite("viewer")}
            disabled={inviteLoading}
          >
            {inviteLoading ? (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            ) : (
              <UserPlus className="mr-2 h-4 w-4" />
            )}
            Invite Viewer
          </Button>
        </div>

        {inviteUrl && (
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <Input
                readOnly
                value={inviteUrl}
                className={expired ? "opacity-50" : ""}
              />
              <Button
                size="sm"
                variant="secondary"
                onClick={copyToClipboard}
                disabled={expired}
              >
                <Copy className="mr-2 h-4 w-4" />
                Copy Link
              </Button>
            </div>

            <div className="flex items-center justify-between text-sm">
              {expired ? (
                <span className="flex items-center gap-1.5 text-destructive font-medium">
                  <Clock className="h-4 w-4" />
                  Expired — generate a new link
                </span>
              ) : (
                <span className="flex items-center gap-1.5 text-warning font-medium">
                  <Clock className="h-4 w-4" />
                  Expires in {secondsLeft}s
                </span>
              )}

              {!expired && (
                <a
                  href={inviteUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 text-primary hover:underline"
                >
                  <LinkIcon className="h-4 w-4" />
                  Open invite page
                </a>
              )}
            </div>
          </div>
        )}
      </GlassCard>

      {/* ── Team Members ── */}
      <GlassCard className="p-6 space-y-5">
        <div>
          <h2 className="font-heading text-lg font-semibold flex items-center gap-2">
            <Users className="h-5 w-5" />
            Team Members
          </h2>
          <p className="text-sm text-muted-foreground">
            View and manage users in your organization.
          </p>
        </div>

        {usersLoading ? (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : users.length === 0 ? (
          <p className="text-sm text-muted-foreground py-4">No users found.</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Email</TableHead>
                <TableHead>Role</TableHead>
                <TableHead className="w-[100px]">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {users.map((user) => (
                <TableRow key={user.id ?? user.email}>
                  <TableCell className="font-medium">{user.email}</TableCell>
                  <TableCell>
                    {user.role === "admin" ? (
                      <Badge>Admin</Badge>
                    ) : (
                      <Select
                        value={user.role}
                        onValueChange={(value) =>
                          user.id != null && handleRoleChange(user.id, value)
                        }
                      >
                        <SelectTrigger className="w-[130px]">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="operator">Operator</SelectItem>
                          <SelectItem value="viewer">Viewer</SelectItem>
                        </SelectContent>
                      </Select>
                    )}
                  </TableCell>
                  <TableCell>
                    {user.role !== "admin" && (
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => {
                          setDeleteTarget(user);
                          setDeleteDialogOpen(true);
                        }}
                      >
                        <Trash2 className="h-4 w-4 text-destructive" />
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Confirm user removal</DialogTitle>
            </DialogHeader>
            <p className="text-sm text-muted-foreground">
              Are you sure you want to remove{" "}
              <span className="font-medium text-foreground">
                {deleteTarget?.email}
              </span>
              ? This action cannot be undone.
            </p>
            <DialogFooter>
              <DialogClose asChild>
                <Button variant="outline">Cancel</Button>
              </DialogClose>
              <Button
                variant="destructive"
                onClick={handleDeleteTeamMember}
                disabled={deletingUser}
              >
                {deletingUser && (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                )}
                Confirm Remove
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </GlassCard>

      {/* ── Remove User ── */}
      <GlassCard className="p-6 space-y-5">
        <div>
          <h2 className="font-heading text-lg font-semibold">Remove User</h2>
        </div>

        <div className="flex flex-wrap items-end gap-3">
          <div className="flex-1 min-w-[240px] space-y-1.5">
            <Label htmlFor="remove-email">Email address</Label>
            <Input
              id="remove-email"
              type="email"
              placeholder="user@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
            <DialogTrigger asChild>
              <Button
                variant="destructive"
                disabled={!email.trim() || removeLoading}
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Remove
              </Button>
            </DialogTrigger>

            <DialogContent>
              <DialogHeader>
                <DialogTitle>Confirm user removal</DialogTitle>
              </DialogHeader>
              <p className="text-sm text-muted-foreground">
                Are you sure you want to remove{" "}
                <span className="font-medium text-foreground">{email}</span>?
                This action cannot be undone.
              </p>
              <DialogFooter>
                <DialogClose asChild>
                  <Button variant="outline">Cancel</Button>
                </DialogClose>
                <Button
                  variant="destructive"
                  onClick={handleRemoveUser}
                  disabled={removeLoading}
                >
                  {removeLoading && (
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  )}
                  Confirm Remove
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
      </GlassCard>
    </div>
  );
}
