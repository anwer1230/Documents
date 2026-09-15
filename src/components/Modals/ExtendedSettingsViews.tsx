import React from 'react';

export const StoriesSettingsView: React.FC<{ onBack?: () => void }> = ({ onBack }) => (
  <div className="p-4">
    {onBack && (
      <button onClick={onBack} className="text-sm text-sky-400 mb-4">
        ← رجوع
      </button>
    )}
    <h3 className="font-bold text-white mb-2">إعدادات القصص (Stories)</h3>
    <p className="text-gray-400 text-sm">إعدادات الخصوصية والتحكم في القصص</p>
  </div>
);

export const MessagesSettingsView: React.FC<{ onBack?: () => void }> = ({ onBack }) => (
  <div className="p-4">
    {onBack && (
      <button onClick={onBack} className="text-sm text-sky-400 mb-4">
        ← رجوع
      </button>
    )}
    <h3 className="font-bold text-white mb-2">إعدادات الرسائل</h3>
    <p className="text-gray-400 text-sm">خيارات إرسال الرسائل والمحادثات المباشرة</p>
  </div>
);

export const TopicsSettingsView: React.FC<{ onBack?: () => void }> = ({ onBack }) => (
  <div className="p-4">
    {onBack && (
      <button onClick={onBack} className="text-sm text-sky-400 mb-4">
        ← رجوع
      </button>
    )}
    <h3 className="font-bold text-white mb-2">إعدادات المواضيع (Topics)</h3>
    <p className="text-gray-400 text-sm">إدارة مواضيع المجموعات العامة والخاصة</p>
  </div>
);

export const SharedMediaView: React.FC<{ onBack?: () => void }> = ({ onBack }) => (
  <div className="p-4">
    {onBack && (
      <button onClick={onBack} className="text-sm text-sky-400 mb-4">
        ← رجوع
      </button>
    )}
    <h3 className="font-bold text-white mb-2">الوسائط المشتركة</h3>
    <p className="text-gray-400 text-sm">عرض الوسائط والمستندات والروابط</p>
  </div>
);

export const AdsSettingsView: React.FC<{ onBack?: () => void }> = ({ onBack }) => (
  <div className="p-4">
    {onBack && (
      <button onClick={onBack} className="text-sm text-sky-400 mb-4">
        ← رجوع
      </button>
    )}
    <h3 className="font-bold text-white mb-2">الإعلانات والرعايات</h3>
    <p className="text-gray-400 text-sm">خيارات ظهور الإعلانات في القنوات العامة</p>
  </div>
);

export const BackupRestoreView: React.FC<{ onBack?: () => void }> = ({ onBack }) => (
  <div className="p-4">
    {onBack && (
      <button onClick={onBack} className="text-sm text-sky-400 mb-4">
        ← رجوع
      </button>
    )}
    <h3 className="font-bold text-white mb-2">النسخ الاحتياطي والاستعادة</h3>
    <p className="text-gray-400 text-sm">حفظ نسخة احتياطية من المحادثات محلياً أو سحابياً</p>
  </div>
);

export const ExtendedSettingsViews: React.FC<{ subPage: string; onBack: () => void }> = ({ subPage, onBack }) => {
  switch (subPage) {
    case 'stories':
      return <StoriesSettingsView onBack={onBack} />;
    case 'messages':
      return <MessagesSettingsView onBack={onBack} />;
    case 'topics':
      return <TopicsSettingsView onBack={onBack} />;
    case 'shared_media':
      return <SharedMediaView onBack={onBack} />;
    case 'ads':
      return <AdsSettingsView onBack={onBack} />;
    case 'backup':
      return <BackupRestoreView onBack={onBack} />;
    default:
      return (
        <div className="p-4">
          <button onClick={onBack} className="text-sm text-sky-400 mb-4">
            ← رجوع
          </button>
          <p className="text-gray-400 text-sm">إعدادات إضافية</p>
        </div>
      );
  }
};
