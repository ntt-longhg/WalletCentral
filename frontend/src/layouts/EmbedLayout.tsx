import React from 'react';
import { Outlet, useSearchParams } from 'react-router-dom';
import { useIframeResize } from '../hooks/useIframeResize';
import { usePostMessageListener } from '../hooks/usePostMessageListener';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { EmbedNotificationProvider } from '../context/EmbedNotificationContext';
import { EmbedAccessDeniedPage } from '../pages/embed/EmbedAccessDeniedPage';
import { Loader2 } from 'lucide-react';

const EmbedContent: React.FC = () => {
  const { token, authReady, validateEmbedToken } = useAuth();
  const [searchParams] = useSearchParams();

  useIframeResize();
  usePostMessageListener();

  if (!authReady) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader2 className="h-8 w-8 text-blue-600 animate-spin" />
      </div>
    );
  }

  const apiKeyFromUrl = searchParams.get('api-key');

  if (!apiKeyFromUrl && !token) {
    return <EmbedAccessDeniedPage />;
  }

  if (token && !validateEmbedToken()) {
    return <EmbedAccessDeniedPage />;
  }

  return (
    <EmbedNotificationProvider>
      <div
        id="embed-root-container"
        className="w-full min-h-screen m-0 box-border bg-transparent font-sans text-slate-900"
      >
        <Outlet />
      </div>
    </EmbedNotificationProvider>
  );
};

export const EmbedLayout: React.FC = () => {
  return (
    <AuthProvider>
      <EmbedContent />
    </AuthProvider>
  );
};
