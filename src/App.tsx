/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { TelegramProvider, useTelegram } from './context/TelegramContext';
import { GlobalErrorBoundary } from './components/Common/GlobalErrorBoundary';
import { Sidebar } from './components/Sidebar/Sidebar';
import { ChatView } from './components/Chat/ChatView';
import { ChatInfoPanel } from './components/RightPanel/ChatInfoPanel';
import { NavigationDrawer } from './components/Sidebar/NavigationDrawer';
import { ApiConfigModal } from './components/Modals/ApiConfigModal';
import { SettingsModal } from './components/Modals/SettingsModal';
import { CallModal } from './components/Modals/CallModal';
import { MediaViewerModal } from './components/Modals/MediaViewerModal';
import { NewChatModal } from './components/Modals/NewChatModal';
import { AddAccountModal } from './components/Modals/AddAccountModal';
import { JoinInviteModal } from './components/Modals/JoinInviteModal';
import { ApkInstallerModal } from './components/Modals/ApkInstallerModal';
import { MiniAppsModal } from './components/Modals/MiniAppsModal';
import { ThemeEditorModal } from './components/Modals/ThemeEditorModal';
import { ExportChatModal } from './components/Modals/ExportChatModal';
import { ContactsModal } from './components/Modals/ContactsModal';
import { LinkMonitorModal } from './components/Modals/LinkMonitorModal';
import { SendOnlyModal } from './components/Modals/SendOnlyModal';
import { PremiumModal } from './components/Modals/PremiumModal';
import { SecretChatInfoModal } from './components/Modals/SecretChatInfoModal';
import { GroupAdminModal } from './components/Modals/GroupAdminModal';
import { ForumTopicsModal } from './components/Modals/ForumTopicsModal';
import { SenderModal } from './components/Modals/SenderModal';
import { MonitorModal } from './components/Modals/MonitorModal';
import { MyMessagesModal } from './components/Modals/MyMessagesModal';
import { AutoJoinerModal } from './components/Modals/AutoJoinerModal';
import { AutoResponderModal } from './components/Modals/AutoResponderModal';
import { SmartAiLearnModal } from './components/Modals/SmartAiLearnModal';
import { ScheduledRotatorModal } from './components/Modals/ScheduledRotatorModal';
import { LiveLinkDiscoverModal } from './components/Modals/LiveLinkDiscoverModal';
import { UserProfileModal } from './components/Modals/UserProfileModal';
import { SalamActivityLog } from './components/SalamActivityLog';
import { ForwardModal } from './components/Interactions/ForwardModal';
import { ChatContextMenuView } from './components/Interactions/ChatContextMenu';
import { MessageContextMenuView } from './components/Interactions/MessageContextMenu';
import { ToastContainer } from './components/Interactions/ToastContainer';
import { InAppNotificationBanner } from './components/Notifications/InAppNotificationBanner';
import { AndroidNotificationShade } from './components/Notifications/AndroidNotificationShade';
import { InstallAppBanner } from './components/Notifications/InstallAppBanner';
import { UpdateNotification } from './components/Notifications/UpdateNotification';
import { TelegramAuthScreen } from './components/Auth/TelegramAuthScreen';
import { useMobileNavigation } from './hooks/useMobileNavigation';
import { AppUpdateAlertDialog } from './components/Modals/AppUpdateAlertDialog';
import { UpdateAppActivityModal } from './components/Modals/UpdateAppActivityModal';
import { RestrictedContentModal } from './components/Modals/RestrictedContentModal';
import { ScreenshotBlockedToast } from './components/Notifications/ScreenshotBlockedToast';
import { NotificationCenter } from './core/NotificationCenter';
import { appUpdateController } from './core/messenger/AppUpdateController';

const TelegramAppContent: React.FC = () => {
  const {
    isAuthenticated,
    inAppNotifications,
    dismissNotification,
    activeModal,
    setActiveModal,
    showToast,
    settings,
    isOffline,
    refreshDialogs,
  } = useTelegram();
  const [showUpdateDialog, setShowUpdateDialog] = React.useState(false);
  const [showUpdateActivity, setShowUpdateActivity] = React.useState(false);
  const isArabic = settings.language === 'ar';

  // Offline-First: Automatic reconnect & background re-sync on network restoration
  React.useEffect(() => {
    const handleOnline = () => {
      // Reconnected automatically: refresh data from cloud without user intervention
      refreshDialogs();
    };
    const handleOffline = () => {
      // Offline mode: non-intrusive strip is displayed automatically via isOffline state
    };
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, [refreshDialogs]);

  // Replicate LaunchActivity.java NotificationCenter observer
  React.useEffect(() => {
    const observer = {
      didReceivedNotification: (id: number | string, account: number, ...args: any[]) => {
        if (id === NotificationCenter.appUpdateAvailable) {
          const update = args[0];
          const isManual = args[1];
          // Check if previously dismissed
          const dismissedVer = localStorage.getItem('tg_dismissed_update_version');
          if (isManual || !dismissedVer || dismissedVer !== update?.version) {
            setShowUpdateDialog(true);
          }
        } else if (id === NotificationCenter.appUpdateNotModified) {
          showToast(isArabic ? 'أنت تستخدم أحدث إصدار من تيليجرام بنجاح' : "You're already using the latest version of Telegram", '✅');
        } else if (id === NotificationCenter.appDidLogout) {
          setActiveModal(null);
        }
      },
    };

    NotificationCenter.getGlobalInstance().addObserver(observer, NotificationCenter.appUpdateAvailable);
    NotificationCenter.getGlobalInstance().addObserver(observer, NotificationCenter.appUpdateNotModified);
    NotificationCenter.getGlobalInstance().addObserver(observer, NotificationCenter.appDidLogout);

    return () => {
      NotificationCenter.getGlobalInstance().removeObserver(observer, NotificationCenter.appUpdateAvailable);
      NotificationCenter.getGlobalInstance().removeObserver(observer, NotificationCenter.appUpdateNotModified);
      NotificationCenter.getGlobalInstance().removeObserver(observer, NotificationCenter.appDidLogout);
    };
  }, [isArabic, showToast]);

  // Activate mobile hardware back button, touch navigation & popstate stack
  useMobileNavigation();

  if (!isAuthenticated) {
    return (
      <div id="tg-auth-wrapper" className="w-screen h-screen min-h-screen bg-[#0e1621] text-white overflow-hidden relative select-none flex flex-col">
        {isOffline && (
          <div
            id="tg-offline-top-strip-auth"
            role="status"
            aria-live="polite"
            className="w-full bg-[#182533] border-b border-amber-500/30 px-3 py-1.5 flex items-center justify-between text-xs text-amber-200 select-none z-50 shrink-0"
          >
            <div className="flex items-center gap-2">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-500"></span>
              </span>
              <span className="font-medium text-[11px] sm:text-xs">
                {isArabic
                  ? 'في انتظار الاتصال بالشبكة... (وضع عدم الاتصال)'
                  : 'Waiting for network... (Offline mode)'}
              </span>
            </div>
          </div>
        )}
        <div className="flex-1 w-full h-full relative overflow-hidden">
          <TelegramAuthScreen />
        </div>
        <ToastContainer />
      </div>
    );
  }

  return (
    <div
      className="fixed inset-0 w-full h-full h-[100dvh] flex flex-col overflow-hidden font-sans select-none"
      style={{
        backgroundColor: 'var(--tg-theme-bg)',
      }}
    >
      {/* Smart In-App Update Notification */}
      <UpdateNotification />

      {/* Telegram Official Offline Top Strip (Non-intrusive, keeps rest of app fully live & readable) */}
      {isOffline && (
        <div
          id="tg-offline-top-strip"
          role="status"
          aria-live="polite"
          className="w-full bg-[#182533] border-b border-amber-500/40 px-3 py-1.5 flex items-center justify-between text-xs text-amber-200 select-none z-50 transition-all duration-300 shadow-sm shrink-0"
        >
          <div className="flex items-center gap-2">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-500"></span>
            </span>
            <span className="font-medium text-[11px] sm:text-xs">
              {isArabic
                ? 'في انتظار الاتصال بالشبكة... (وضع عدم الاتصال: عرض المحادثات والرسائل المحفوظة محلياً)'
                : 'Waiting for network... (Offline mode: viewing locally cached chats & messages)'}
            </span>
          </div>
          <button
            onClick={() => refreshDialogs()}
            className="px-2.5 py-0.5 rounded-full bg-white/10 hover:bg-white/20 active:bg-white/30 text-[10px] sm:text-[11px] font-semibold text-white transition-colors cursor-pointer"
          >
            {isArabic ? 'إعادة المحاولة' : 'Retry'}
          </button>
        </div>
      )}

      <div
        id="tg-app-root"
        className="flex-1 w-full h-full flex overflow-hidden relative"
      >
        {/* Left Sidebar (Chats, Folders, Search) */}
        <Sidebar />

      {/* Center Chat Feed / Message View */}
      <ChatView />

      {/* Right Shared Media & Details Info Panel */}
      <ChatInfoPanel />

      {/* Drawer Slide-out Menu */}
      <NavigationDrawer />

      {/* Dialogs & Overlays */}
      <ApiConfigModal />
      <SettingsModal />
      <CallModal />
      <MediaViewerModal />
      <NewChatModal />
      <AddAccountModal />
      <JoinInviteModal />
      <ApkInstallerModal />
      <MiniAppsModal
        isOpen={activeModal === 'mini-apps'}
        onClose={() => setActiveModal('none')}
      />
      <ThemeEditorModal
        isOpen={activeModal === 'theme-editor'}
        onClose={() => setActiveModal('none')}
      />
      <ExportChatModal
        isOpen={activeModal === 'export-chat'}
        onClose={() => setActiveModal('none')}
      />
      <ContactsModal
        isOpen={activeModal === 'contacts'}
        onClose={() => setActiveModal('none')}
      />
      <LinkMonitorModal
        isOpen={activeModal === 'link-monitor'}
        onClose={() => setActiveModal('none')}
      />
      <SendOnlyModal />
      <PremiumModal />
      <SecretChatInfoModal />
      <GroupAdminModal />
      <ForumTopicsModal />
      
      {/* 7 Core Telegram Functions (Activities) */}
      <SenderModal />
      <MonitorModal />
      <MyMessagesModal />
      <AutoJoinerModal />
      <AutoResponderModal />
      <SmartAiLearnModal />
      <ScheduledRotatorModal />
      <LiveLinkDiscoverModal />
      <UserProfileModal />

      {/* Salam Mode Real-Time Activity Log */}
      <SalamActivityLog
        isOpen={activeModal === 'salam-activity-log'}
        onClose={() => setActiveModal('none')}
      />

      <ForwardModal />

      {/* Dynamic Context Menus */}
      <ChatContextMenuView />
      <MessageContextMenuView />

      {/* Telegram Official App Update Alert Dialog & Full Download Activity */}
      <AppUpdateAlertDialog
        isOpen={showUpdateDialog}
        onClose={() => setShowUpdateDialog(false)}
        onOpenFullActivity={() => {
          setShowUpdateDialog(false);
          setShowUpdateActivity(true);
        }}
      />
      <UpdateAppActivityModal
        isOpen={showUpdateActivity}
        onClose={() => setShowUpdateActivity(false)}
      />

      {/* Android Notification Shade (Pull-down & Background Notifications) */}
      <AndroidNotificationShade
        isOpen={activeModal === 'android-notification-shade'}
        onClose={() => setActiveModal('none')}
      />

      {/* AI Studio Style Direct App Installation Banner */}
      <InstallAppBanner />

      {/* In-App Floating Heads-up Notification Banner */}
      <InAppNotificationBanner
        notifications={inAppNotifications}
        onDismiss={dismissNotification}
      />

      {/* Restricted Content Warning Modal */}
      <RestrictedContentModal
        isOpen={activeModal === 'restricted-content'}
        onClose={() => setActiveModal('none')}
      />

      {/* Floating Toast Notifications */}
      <ToastContainer />

      {/* Android FLAG_SECURE Screenshot Blocked Alert */}
      <ScreenshotBlockedToast />
      </div>
    </div>
  );
};

export default function App() {
  return (
    <GlobalErrorBoundary>
      <TelegramProvider>
        <TelegramAppContent />
      </TelegramProvider>
    </GlobalErrorBoundary>
  );
}
