import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { TablePagination } from '@/components/ui/pagination';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { useToast } from '@/components/ui/toast';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow, TableSkeleton } from '@/components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import type { RoleResponse, PermissionResponse } from '@/types/api';
import {
  Shield,
  Users,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  Edit,
  Loader2,
  UserCheck,
  Key,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { useAuthStore } from '@/stores';
import { formatDate } from '@/lib/utils';

export const AdminRbacPage: React.FC = () => {
  const { addToast } = useToast();
  const {
    users,
    roles,
    permissions,
    usersLoading: loading,
    fetchUsers,
    fetchRoles,
    fetchPermissions,
    createRole,
    updateRole,
    deleteRole,
    assignRole,
    removeRole,
    grantPermission,
  } = useAuthStore();
  const [activeTab, setActiveTab] = useState('users');
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);

  // Role dialog
  const [showRoleDialog, setShowRoleDialog] = useState(false);
  const [editingRole, setEditingRole] = useState<{ id: string; name: string; description?: string; permissionIds: string[]; isSystem: boolean } | null>(null);
  const [roleForm, setRoleForm] = useState({ name: '', description: '', permissionIds: [] as string[] });

  // Assign role dialog
  const [showAssignDialog, setShowAssignDialog] = useState<{ email: string } | null>(null);
  const [selectedRoleId, setSelectedRoleId] = useState('');

  // Permission override dialog
  const [showPermDialog, setShowPermDialog] = useState<{ email: string } | null>(null);
  const [selectedPermId, setSelectedPermId] = useState('');

  const fetchData = useCallback(async () => {
    await Promise.allSettled([fetchUsers(), fetchRoles(), fetchPermissions()]);
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const filteredUsers = useMemo(() => {
    return users.filter(u =>
      !searchTerm || u.email.toLowerCase().includes(searchTerm.toLowerCase())
    );
  }, [users, searchTerm]);

  const totalPages = Math.max(1, Math.ceil(filteredUsers.length / pageSize));
  const hasPrevious = currentPage > 1;
  const hasNext = currentPage < totalPages;

  const paginatedUsers = useMemo(() => {
    const startIndex = (currentPage - 1) * pageSize;
    return filteredUsers.slice(startIndex, startIndex + pageSize);
  }, [filteredUsers, currentPage, pageSize]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, pageSize]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  // ====================== ROLE CRUD ======================

  const handleCreateRole = () => {
    setEditingRole(null);
    setRoleForm({ name: '', description: '', permissionIds: [] });
    setShowRoleDialog(true);
  };

  const handleEditRole = (role: RoleResponse) => {
    setEditingRole(role);
    setRoleForm({
      name: role.name,
      description: role.description || '',
      permissionIds: role.permissionIds,
    });
    setShowRoleDialog(true);
  };

  const handleSaveRole = async () => {
    try {
      if (editingRole) {
        await updateRole(editingRole.id, roleForm);
        addToast({ variant: 'success', message: 'Cập nhật role thành công!' });
      } else {
        await createRole(roleForm);
        addToast({ variant: 'success', message: 'Tạo role mới thành công!' });
      }
      setShowRoleDialog(false);
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Lỗi thao tác role.' });
    }
  };

  const handleDeleteRole = async (role: { id: string; name: string; isSystem: boolean }) => {
    if (role.isSystem) {
      addToast({ variant: 'destructive', message: 'Không thể xóa role hệ thống.' });
      return;
    }
    try {
      await deleteRole(role.id);
      addToast({ variant: 'success', message: `Đã xóa role "${role.name}".` });
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Lỗi xóa role.' });
    }
  };

  const togglePermission = (permId: string) => {
    setRoleForm(prev => ({
      ...prev,
      permissionIds: prev.permissionIds.includes(permId)
        ? prev.permissionIds.filter(id => id !== permId)
        : [...prev.permissionIds, permId],
    }));
  };

  // ====================== USER ROLE ASSIGNMENT ======================

  const handleAssignRole = async () => {
    if (!showAssignDialog || !selectedRoleId) return;
    try {
      await assignRole(showAssignDialog.email, selectedRoleId);
      addToast({ variant: 'success', message: `Đã gán role cho ${showAssignDialog.email}` });
      setShowAssignDialog(null);
      fetchUsers();
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Lỗi gán role.' });
    }
  };

  const handleRemoveRole = async (email: string) => {
    try {
      await removeRole(email);
      addToast({ variant: 'success', message: `Đã gỡ role của ${email}` });
      fetchUsers();
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Lỗi gỡ role.' });
    }
  };

  // ====================== PERMISSION OVERRIDE ======================

  const handleGrantPermission = async () => {
    if (!showPermDialog || !selectedPermId) return;
    try {
      await grantPermission(showPermDialog.email, selectedPermId);
      addToast({ variant: 'success', message: 'Đã cấp quyền thêm.' });
      setShowPermDialog(null);
      fetchUsers();
    } catch (err: any) {
      addToast({ variant: 'destructive', message: err.response?.data?.message || 'Lỗi cấp quyền.' });
    }
  };

  // Group permissions by module
  const groupedPerms = permissions.reduce<Record<string, PermissionResponse[]>>((acc, p) => {
    if (!acc[p.module]) acc[p.module] = [];
    acc[p.module].push(p);
    return acc;
  }, {});

  const columns = [
    {
      header: 'Email',
      accessor: 'email',
      widthClass: 'w-[200px]',
    },
    {
      header: 'Tên',
      accessor: 'name',
      widthClass: 'w-[140px]',
    },
    {
      header: 'Role',
      accessor: 'role',
      widthClass: 'w-[140px]',
    },
    {
      header: 'Trạng thái',
      accessor: 'status',
      widthClass: 'w-[140px]',
    },
    {
      header: 'Đăng nhập cuối',
      accessor: 'lastLogin',
      widthClass: 'w-[160px]',
    },
    {
      header: 'Thao tác',
      accessor: 'actions',
      widthClass: 'w-[140px]',
    },
  ]

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Shield className="h-5 w-5 text-blue-600" />
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Quản lý RBAC</h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Quản lý vai trò, quyền hạn và phân quyền cho các tài khoản Admin.
          </p>
        </div>
        <Button variant="outline" onClick={fetchData} disabled={loading} className="gap-1.5">
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
        </Button>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="users" className="gap-2"><Users className="h-4 w-4" /> Users</TabsTrigger>
          <TabsTrigger value="roles" className="gap-2"><Shield className="h-4 w-4" /> Roles</TabsTrigger>
          <TabsTrigger value="permissions" className="gap-2"><Key className="h-4 w-4" /> Permissions</TabsTrigger>
        </TabsList>

        {/* ====================== USERS TAB ====================== */}
        <TabsContent value="users" className="space-y-4">
          <div className="relative max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <Input
              placeholder="Tìm kiếm user theo email..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-9"
            />
          </div>

          <Card className="border-slate-200 shadow-sm">
            <CardHeader className="pb-3">
              <CardTitle className="text-base font-semibold flex items-center gap-2">
                <Users className="h-4 w-4 text-blue-600" /> Danh sách Users ({filteredUsers.length})
              </CardTitle>
            </CardHeader>
            <CardContent>
              {filteredUsers.length === 0 ? (
                <div className="text-center py-10 text-slate-400 text-sm">Chưa có user nào.</div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      {
                        columns.map((column) => (
                          <TableHead key={column.accessor} className={column.widthClass}>
                            {column.header}
                          </TableHead>
                        ))
                      }
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {
                      loading ? (
                        <TableSkeleton columns={columns.length} rows={pageSize} />
                      ) : (
                        <>
                          {paginatedUsers.map((user) => (
                            <TableRow key={user.id}>
                              <TableCell className="font-mono text-sm font-medium">{user.email}</TableCell>
                              <TableCell className="text-sm">{user.displayName || '-'}</TableCell>
                              <TableCell>
                                {user.roleName ? (
                                  <Badge variant={user.roleName === 'SUPER_ADMIN' ? 'destructive' : 'default'}>
                                    {user.roleName}
                                  </Badge>
                                ) : (
                                  <Badge variant="outline">Chưa gán</Badge>
                                )}
                              </TableCell>
                              <TableCell>
                                <Badge variant={user.isActive ? 'default' : 'secondary'}>
                                  {user.isActive ? 'Active' : 'Inactive'}
                                </Badge>
                              </TableCell>
                              <TableCell className="text-xs text-slate-500">
                                {user.lastLoginAt ? formatDate(user.lastLoginAt) : 'Chưa đăng nhập'}
                              </TableCell>
                              <TableCell>
                                <div className="flex items-center gap-1.5">
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    className="text-xs h-7"
                                    onClick={() => {
                                      setShowAssignDialog(user);
                                      setSelectedRoleId(user.roleId || '');
                                    }}
                                  >
                                    <UserCheck className="h-3.5 w-3.5 mr-1" /> Role
                                  </Button>
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    className="text-xs h-7"
                                    onClick={() => setShowPermDialog(user)}
                                  >
                                    <Key className="h-3.5 w-3.5 mr-1" /> Quyền
                                  </Button>
                                </div>
                              </TableCell>
                            </TableRow>
                          ))}
                        </>
                      )
                    }
                  </TableBody>
                </Table>
              )}

              {filteredUsers.length > 0 && (
                <TablePagination
                  pageSize={pageSize}
                  onPageSizeChange={(nextSize) => {
                    setPageSize(nextSize);
                    setCurrentPage(1);
                  }}
                  onPrevious={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
                  onNext={() => setCurrentPage((prev) => Math.min(totalPages, prev + 1))}
                  hasPrevious={hasPrevious}
                  hasNext={hasNext}
                  summaryText={`${filteredUsers.length} user trong hệ thống`}
                  pageSizeOptions={[5, 10, 20, 50]}
                />
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* ====================== ROLES TAB ====================== */}
        <TabsContent value="roles" className="space-y-4">
          <div className="flex justify-end">
            <Button onClick={handleCreateRole} className="gap-2 bg-blue-600 hover:bg-blue-700">
              <Plus className="h-4 w-4" /> Tạo Role Mới
            </Button>
          </div>

          <Card className="border-slate-200 shadow-sm">
            <CardContent className="pt-6">
              {roles.length === 0 ? (
                <div className="text-center py-10 text-slate-400 text-sm">Không có role nào.</div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Tên Role</TableHead>
                      <TableHead>Mô tả</TableHead>
                      <TableHead>Số quyền</TableHead>
                      <TableHead>Hệ thống</TableHead>
                      <TableHead>Thao tác</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {roles.map((role) => (
                      <TableRow key={role.id}>
                        <TableCell className="font-semibold">{role.name}</TableCell>
                        <TableCell className="text-sm text-slate-600">{role.description || '-'}</TableCell>
                        <TableCell>
                          <Badge variant="secondary">{role.permissionIds.length} quyền</Badge>
                        </TableCell>
                        <TableCell>
                          {role.isSystem && <Badge variant="destructive">System</Badge>}
                        </TableCell>
                        <TableCell>
                          {!role.isSystem && (
                            <div className="flex items-center gap-1.5">
                              <Button size="sm" variant="outline" className="text-xs h-7" onClick={() => handleEditRole(role)}>
                                <Edit className="h-3.5 w-3.5" />
                              </Button>
                              <Button size="sm" variant="destructive" className="text-xs h-7" onClick={() => handleDeleteRole(role)}>
                                <Trash2 className="h-3.5 w-3.5" />
                              </Button>
                            </div>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* ====================== PERMISSIONS TAB ====================== */}
        <TabsContent value="permissions" className="space-y-4">
          <Card className="border-slate-200 shadow-sm">
            <CardHeader className="pb-3">
              <CardTitle className="text-base font-semibold">Tất cả Quyền ({permissions.length})</CardTitle>
            </CardHeader>
            <CardContent>
              {Object.entries(groupedPerms).map(([module, perms]) => (
                <div key={module} className="mb-4">
                  <h4 className="text-sm font-semibold text-slate-700 mb-2 uppercase tracking-wide">{module}</h4>
                  <div className="flex flex-wrap gap-2">
                    {perms.map((p: PermissionResponse) => (
                      <Badge key={p.id} variant="secondary" className="text-xs">
                        {p.code}
                      </Badge>
                    ))}
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* ====================== ASSIGN ROLE DIALOG ====================== */}
      <Dialog open={!!showAssignDialog} onOpenChange={() => setShowAssignDialog(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Gán Role cho User</DialogTitle>
            <DialogDescription>
              Chọn role cho <strong>{showAssignDialog?.email}</strong>
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            {roles.map(role => (
              <label
                key={role.id}
                className={`flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition-colors ${
                  selectedRoleId === role.id ? 'border-blue-500 bg-blue-50' : 'border-slate-200 hover:bg-slate-50'
                }`}
              >
                <input
                  type="radio"
                  name="role"
                  value={role.id}
                  checked={selectedRoleId === role.id}
                  onChange={() => setSelectedRoleId(role.id)}
                  className="accent-blue-600"
                />
                <div>
                  <div className="font-medium text-sm">{role.name}</div>
                  <div className="text-xs text-slate-500">{role.description}</div>
                </div>
              </label>
            ))}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowAssignDialog(null)}>Hủy</Button>
            <Button onClick={handleAssignRole} disabled={!selectedRoleId}>Gán Role</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ====================== CREATE/EDIT ROLE DIALOG ====================== */}
      <Dialog open={showRoleDialog} onOpenChange={setShowRoleDialog}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editingRole ? 'Chỉnh sửa Role' : 'Tạo Role Mới'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Tên Role</Label>
              <Input
                value={roleForm.name}
                onChange={(e) => setRoleForm({ ...roleForm, name: e.target.value })}
                placeholder="e.g. CONTENT_MANAGER"
              />
            </div>
            <div className="space-y-2">
              <Label>Mô tả</Label>
              <Input
                value={roleForm.description}
                onChange={(e) => setRoleForm({ ...roleForm, description: e.target.value })}
                placeholder="Mô tả ngắn về role"
              />
            </div>
            <div className="space-y-2">
              <Label>Quyền ({roleForm.permissionIds.length} đã chọn)</Label>
              <div className="max-h-60 overflow-y-auto border rounded-lg p-3 space-y-3">
                {Object.entries(groupedPerms).map(([module, perms]) => (
                  <div key={module}>
                    <div className="flex items-center gap-2 mb-1">
                      <input
                        type="checkbox"
                        checked={perms.every((p: PermissionResponse) => roleForm.permissionIds.includes(p.id))}
                        onChange={() => {
                          const allSelected = perms.every((p: PermissionResponse) => roleForm.permissionIds.includes(p.id));
                          const newIds = allSelected
                            ? roleForm.permissionIds.filter(id => !perms.some((p: PermissionResponse) => p.id === id))
                            : [...roleForm.permissionIds, ...perms.map((p: PermissionResponse) => p.id)];
                          setRoleForm({ ...roleForm, permissionIds: newIds });
                        }}
                        className="accent-blue-600"
                      />
                      <span className="text-xs font-semibold text-slate-600 uppercase">{module}</span>
                    </div>
                    <div className="flex flex-wrap gap-1.5 ml-5">
                      {perms.map((p: PermissionResponse) => (
                        <label key={p.id} className="flex items-center gap-1 cursor-pointer">
                          <input
                            type="checkbox"
                            checked={roleForm.permissionIds.includes(p.id)}
                            onChange={() => togglePermission(p.id)}
                            className="accent-blue-600"
                          />
                          <span className="text-xs text-slate-600">{p.code}</span>
                        </label>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowRoleDialog(false)}>Hủy</Button>
            <Button onClick={handleSaveRole}>
              {editingRole ? 'Cập nhật' : 'Tạo Role'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ====================== PERMISSION OVERRIDE DIALOG ====================== */}
      <Dialog open={!!showPermDialog} onOpenChange={() => setShowPermDialog(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Thêm Quyền Override</DialogTitle>
            <DialogDescription>
              Cấp quyền bổ sung cho <strong>{showPermDialog?.email}</strong> (ngoài quyền từ role)
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <Label>Chọn quyền</Label>
            <div className="max-h-60 overflow-y-auto border rounded-lg p-3 space-y-2">
              {Object.entries(groupedPerms).map(([module, perms]) => (
                <div key={module}>
                  <div className="text-xs font-semibold text-slate-500 uppercase mb-1">{module}</div>
                  {perms.map((p: PermissionResponse) => (
                    <label key={p.id} className="flex items-center gap-2 cursor-pointer py-0.5">
                      <input
                        type="radio"
                        name="perm"
                        value={p.id}
                        checked={selectedPermId === p.id}
                        onChange={() => setSelectedPermId(p.id)}
                        className="accent-blue-600"
                      />
                      <span className="text-xs">{p.code}</span>
                      {p.description && <span className="text-xs text-slate-400">- {p.description}</span>}
                    </label>
                  ))}
                </div>
              ))}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowPermDialog(null)}>Hủy</Button>
            <Button onClick={handleGrantPermission} disabled={!selectedPermId}>Cấp quyền</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
