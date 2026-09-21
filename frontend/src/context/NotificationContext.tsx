import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { useWebSocket } from '../hooks/useWebSocket';
import { useToast } from '../components/ui/toast';
import { walletPlanService } from '../services/billingServices';
import { useBillingStore } from '@/stores/useBillingStore';

interface Notification {
  id: string;
  tenantId: string;
  type: string;
  title: string;
  message: string;
  referenceType?: string;
  referenceId?: string;
  isRead: boolean;
  readAt?: string;
  createdAt: string;
}

interface NotificationContextType {
  notifications: Notification[];
  unreadCount: number;
  isConnected: boolean;
  pendingWalletPlanCount: number;
  addNotification: (notification: Notification) => void;
  markAsRead: (id: string) => void;
  markAllAsRead: () => void;
  clearNotifications: () => void;
  // refreshPendingCount: () => void;
}

const NotificationContext = createContext<NotificationContextType>({
  notifications: [],
  unreadCount: 0,
  isConnected: false,
  pendingWalletPlanCount: 0,
  addNotification: () => { },
  markAsRead: () => { },
  markAllAsRead: () => { },
  clearNotifications: () => { },
  // refreshPendingCount: () => { },
});

export const NotificationProvider: React.FC<{ children: React.ReactNode; tenantId?: string; admin?: boolean }> = ({ children, tenantId, admin }) => {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [pendingWalletPlanCount, setPendingWalletPlanCount] = useState(0);
  const { addToast } = useToast();
   const { pendingPlans, plansLoading: loading, fetchPendingWalletPlans } = useBillingStore();

  const handleNotification = useCallback((notification: Notification) => {
    setNotifications(prev => [notification, ...prev].slice(0, 100));

    if (admin) {
      addToast({
        variant: 'info',
        title: notification.title,
        message: notification.message,
        duration: 5000,
      });

      if (notification.type === 'WALLET_PLAN' || notification.referenceType === 'WALLET_PLAN') {
        // setPendingWalletPlanCount(prev => prev + 1);
        fetchPendingWalletPlans();
      }
    } else {
      addToast({
        variant: 'info',
        title: notification.title,
        message: notification.message,
        duration: 5000,
      });
    }
  }, [admin, addToast]);

  const { isConnected } = useWebSocket({
    tenantId,
    admin,
    onNotification: handleNotification,
    enabled: !!(tenantId || admin),
  });

  const unreadCount = notifications.filter(n => !n.isRead).length;

  // const refreshPendingCount = useCallback(async () => {
  //   if (!admin) return;
  //   try {
  //     const res = await walletPlanService.getPending();
  //     if (res.data.success && res.data?.data?.items) {
  //       setPendingWalletPlanCount(res.data.data.items.length);
  //     }
  //   } catch {
  //     // ignore
  //   }
  // }, [admin]);

  // useEffect(() => {
  //   if (admin) {
  //     refreshPendingCount();
  //     const interval = setInterval(refreshPendingCount, 30000);
  //     return () => clearInterval(interval);
  //   }
  // }, [admin, refreshPendingCount]);

  useEffect(() => {
    if (admin) {
      setPendingWalletPlanCount(pendingPlans.length);
    }
  }, [admin, pendingPlans]);


  const addNotification = useCallback((notification: Notification) => {
    setNotifications(prev => [notification, ...prev].slice(0, 100));
  }, []);

  const markAsRead = useCallback((id: string) => {
    setNotifications(prev =>
      prev.map(n => (n.id === id ? { ...n, isRead: true, readAt: new Date().toISOString() } : n))
    );
  }, []);

  const markAllAsRead = useCallback(() => {
    setNotifications(prev =>
      prev.map(n => ({ ...n, isRead: true, readAt: new Date().toISOString() }))
    );
  }, []);

  const clearNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  return (
    <NotificationContext.Provider
      value={{ notifications, unreadCount, isConnected, pendingWalletPlanCount, addNotification, markAsRead, markAllAsRead, clearNotifications }}
    >
      {children}
    </NotificationContext.Provider>
  );
};

export const useNotifications = () => useContext(NotificationContext);
