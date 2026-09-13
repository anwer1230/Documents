import React, { useState, useEffect, useCallback } from 'react';
import {
  Shield,
  Phone,
  Clock,
  Share2,
  RefreshCw,
  CheckCircle2,
  AlertCircle,
  Lock,
  UserCheck,
  Users,
  UserX,
  ChevronRight,
  Info,
} from 'lucide-react';
import { Api } from '../services/api';

export type PrivacyOption = 'everybody' | 'contacts' | 'nobody';

export interface PrivacySettingsProps {
  onClose?: () => void;
  isAr?: boolean;
  themeConfig?: {
    isDark?: boolean;
    [key: string]: any;
  };
  className?: string;
}

interface PrivacyFieldConfig {
  id: 'phoneNumber' | 'statusTimestamp' | 'forwards';
  titleAr: string;
  titleEn: string;
  descAr: string;
  descEn: string;
  icon: React.ComponentType<{ className?: string }>;
  keyConstructor: () => any;
}

const PRIVACY_FIELDS: PrivacyFieldConfig[] = [
  {
    id: 'phoneNumber',
    titleAr: 'رقم الهاتف',
    titleEn: 'Phone Number',
    descAr: 'من يستطيع رؤية رقم هاتفي والوصول إلي من خلاله',
    descEn: 'Who can see your phone number and find you by it',
    icon: Phone,
    keyConstructor: () => Api.InputPrivacyKeyPhoneNumber(),
  },
  {
    id: 'statusTimestamp',
    titleAr: 'آخر ظهور ومتصل الآن',
    titleEn: 'Last Seen & Online',
    descAr: 'من يستطيع معرفة وقت آخر ظهور لك أو رؤية حالتك متصل',
    descEn: 'Who can see your exact last seen time and online status',
    icon: Clock,
    keyConstructor: () => Api.InputPrivacyKeyStatusTimestamp(),
  },
  {
    id: 'forwards',
    titleAr: 'الرسائل المحولة',
    titleEn: 'Forwarded Messages',
    descAr: 'من يستطيع تضمين رابط لحسابك الشخصي عند إعادة توجيه رسائلك',
    descEn: 'Who can add a link back to your account when forwarding messages',
    icon: Share2,
    keyConstructor: () => Api.InputPrivacyKeyForwards(),
  },
];

export const PrivacySettings: React.FC<PrivacySettingsProps> = ({
  onClose,
  isAr = true,
  themeConfig = { isDark: true },
  className = '',
}) => {
  const isDark = themeConfig?.isDark ?? true;

  // Privacy states for the 3 categories
  const [phonePrivacy, setPhonePrivacy] = useState<PrivacyOption>('contacts');
  const [lastSeenPrivacy, setLastSeenPrivacy] = useState<PrivacyOption>('everybody');
  const [forwardsPrivacy, setForwardsPrivacy] = useState<PrivacyOption>('everybody');

  // Loading and feedback states
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [updatingKey, setUpdatingKey] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [lastSyncedAt, setLastSyncedAt] = useState<Date | null>(null);

  const showNotification = (type: 'success' | 'error', message: string) => {
    setFeedback({ type, message });
    setTimeout(() => {
      setFeedback(null);
    }, 3500);
  };

  /**
   * Fetches cloud privacy settings utilizing Api.account.GetPrivacy
   */
  const fetchAllPrivacy = useCallback(async () => {
    setIsLoading(true);
    try {
      // 1. Phone Number Privacy
      const phoneRes: any = await Api.account.GetPrivacy({
        key: Api.InputPrivacyKeyPhoneNumber(),
      });
      if (phoneRes?.option) {
        setPhonePrivacy(phoneRes.option);
      } else if (phoneRes?.rules?.[0]) {
        const ruleName = phoneRes.rules[0]._ || '';
        if (ruleName.includes('DisallowAll')) setPhonePrivacy('nobody');
        else if (ruleName.includes('AllowContacts')) setPhonePrivacy('contacts');
        else setPhonePrivacy('everybody');
      }

      // 2. Last Seen & Online Privacy
      const statusRes: any = await Api.account.GetPrivacy({
        key: Api.InputPrivacyKeyStatusTimestamp(),
      });
      if (statusRes?.option) {
        setLastSeenPrivacy(statusRes.option);
      } else if (statusRes?.rules?.[0]) {
        const ruleName = statusRes.rules[0]._ || '';
        if (ruleName.includes('DisallowAll')) setLastSeenPrivacy('nobody');
        else if (ruleName.includes('AllowContacts')) setLastSeenPrivacy('contacts');
        else setLastSeenPrivacy('everybody');
      }

      // 3. Forwarded Messages Privacy
      const forwardsRes: any = await Api.account.GetPrivacy({
        key: Api.InputPrivacyKeyForwards(),
      });
      if (forwardsRes?.option) {
        setForwardsPrivacy(forwardsRes.option);
      } else if (forwardsRes?.rules?.[0]) {
        const ruleName = forwardsRes.rules[0]._ || '';
        if (ruleName.includes('DisallowAll')) setForwardsPrivacy('nobody');
        else if (ruleName.includes('AllowContacts')) setForwardsPrivacy('contacts');
        else setForwardsPrivacy('everybody');
      }

      setLastSyncedAt(new Date());
    } catch (err: any) {
      console.error('[PrivacySettings] Error fetching cloud privacy:', err);
      showNotification(
        'error',
        isAr ? 'فشل جلب إعدادات الخصوصية من سيرفرات تيليجرام' : 'Failed to load privacy settings from Telegram Cloud'
      );
    } finally {
      setIsLoading(false);
    }
  }, [isAr]);

  useEffect(() => {
    fetchAllPrivacy();
  }, [fetchAllPrivacy]);

  /**
   * Updates privacy rule on Telegram Cloud utilizing Api.account.SetPrivacy
   */
  const handleUpdatePrivacy = async (
    fieldId: 'phoneNumber' | 'statusTimestamp' | 'forwards',
    newOption: PrivacyOption
  ) => {
    // Optimistic UI update
    if (fieldId === 'phoneNumber') setPhonePrivacy(newOption);
    else if (fieldId === 'statusTimestamp') setLastSeenPrivacy(newOption);
    else if (fieldId === 'forwards') setForwardsPrivacy(newOption);

    setUpdatingKey(fieldId);

    try {
      // Map option to MTProto rule constructors
      let rulesArray: any[] = [];
      if (newOption === 'everybody') {
        rulesArray = [Api.InputPrivacyValueAllowAll()];
      } else if (newOption === 'contacts') {
        rulesArray = [Api.InputPrivacyValueAllowContacts()];
      } else {
        rulesArray = [Api.InputPrivacyValueDisallowAll()];
      }

      // Select key constructor
      let keyConstructor = Api.InputPrivacyKeyPhoneNumber;
      if (fieldId === 'statusTimestamp') keyConstructor = Api.InputPrivacyKeyStatusTimestamp;
      if (fieldId === 'forwards') keyConstructor = Api.InputPrivacyKeyForwards;

      // Invoke Api.account.SetPrivacy
      await Api.account.SetPrivacy({
        key: keyConstructor(),
        rules: rulesArray,
      });

      const fieldLabel =
        fieldId === 'phoneNumber'
          ? (isAr ? 'خصوصية رقم الهاتف' : 'Phone number privacy')
          : fieldId === 'statusTimestamp'
          ? (isAr ? 'خصوصية آخر ظهور' : 'Last seen privacy')
          : (isAr ? 'خصوصية الرسائل المحولة' : 'Forwarded messages privacy');

      showNotification(
        'success',
        isAr
          ? `تم تحديث ${fieldLabel} بنجاح عبر Api.account.SetPrivacy`
          : `${fieldLabel} updated successfully via Api.account.SetPrivacy`
      );
      setLastSyncedAt(new Date());
    } catch (err: any) {
      console.error(`[PrivacySettings] Error setting privacy for ${fieldId}:`, err);
      showNotification(
        'error',
        isAr
          ? 'تعذر حفظ الإعداد الجديد على تيليجرام، تم التراجع'
          : 'Could not sync setting with Telegram Cloud, reverted'
      );
      // Revert from cloud
      fetchAllPrivacy();
    } finally {
      setUpdatingKey(null);
    }
  };

  const getOptionValue = (fieldId: 'phoneNumber' | 'statusTimestamp' | 'forwards'): PrivacyOption => {
    if (fieldId === 'phoneNumber') return phonePrivacy;
    if (fieldId === 'statusTimestamp') return lastSeenPrivacy;
    return forwardsPrivacy;
  };

  const options: { id: PrivacyOption; labelAr: string; labelEn: string; icon: React.ComponentType<{ className?: string }> }[] = [
    { id: 'everybody', labelAr: 'الجميع', labelEn: 'Everybody', icon: Users },
    { id: 'contacts', labelAr: 'جهات اتصالي', labelEn: 'My Contacts', icon: UserCheck },
    { id: 'nobody', labelAr: 'لا أحد', labelEn: 'Nobody', icon: UserX },
  ];

  return (
    <div
      className={`flex flex-col h-full overflow-hidden ${
        isDark ? 'bg-[#17212b] text-white' : 'bg-white text-gray-900'
      } ${className}`}
      dir={isAr ? 'rtl' : 'ltr'}
    >
      {/* Header */}
      <div
        className={`p-4 flex items-center justify-between border-b ${
          isDark ? 'border-gray-800 bg-[#17212b]' : 'border-gray-200 bg-white'
        }`}
      >
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-[#3390ec]/10 text-[#3390ec]">
            <Shield className="w-5 h-5" />
          </div>
          <div>
            <h2 className="font-bold text-base leading-tight">
              {isAr ? 'إعدادات الخصوصية السحابية' : 'Cloud Privacy Settings'}
            </h2>
            <div className="text-xs text-gray-400 flex items-center gap-1.5 mt-0.5">
              <span>Api.account.GetPrivacy / SetPrivacy</span>
              {lastSyncedAt && (
                <>
                  <span>•</span>
                  <span>
                    {isAr ? 'آخر مزامنة:' : 'Synced:'}{' '}
                    {lastSyncedAt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </>
              )}
            </div>
          </div>
        </div>

        <button
          type="button"
          onClick={fetchAllPrivacy}
          disabled={isLoading}
          className={`p-2 rounded-xl border transition flex items-center gap-1.5 text-xs font-medium ${
            isDark
              ? 'border-gray-700 hover:bg-[#232e3c] text-gray-300'
              : 'border-gray-200 hover:bg-gray-50 text-gray-700'
          }`}
          title={isAr ? 'إعادة المزامنة مع تيليجرام' : 'Resync with Telegram Cloud'}
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin text-[#3390ec]' : ''}`} />
          <span className="hidden sm:inline">{isLoading ? (isAr ? 'جارٍ التحديث...' : 'Syncing...') : (isAr ? 'تحديث' : 'Refresh')}</span>
        </button>
      </div>

      {/* Main Body */}
      <div className="flex-1 overflow-y-auto p-4 space-y-5">
        {/* Feedback Alert */}
        {feedback && (
          <div
            className={`p-3 rounded-xl border text-xs flex items-center gap-2.5 transition animate-fadeIn ${
              feedback.type === 'success'
                ? 'bg-green-500/10 border-green-500/30 text-green-400'
                : 'bg-red-500/10 border-red-500/30 text-red-400'
            }`}
          >
            {feedback.type === 'success' ? (
              <CheckCircle2 className="w-4 h-4 shrink-0" />
            ) : (
              <AlertCircle className="w-4 h-4 shrink-0" />
            )}
            <span className="font-medium">{feedback.message}</span>
          </div>
        )}

        {/* Info Banner */}
        <div
          className={`p-3.5 rounded-2xl border text-xs leading-relaxed flex items-start gap-3 ${
            isDark ? 'bg-[#232e3c]/50 border-gray-800 text-gray-300' : 'bg-blue-50/60 border-blue-100 text-gray-700'
          }`}
        >
          <Info className="w-4 h-4 text-[#3390ec] shrink-0 mt-0.5" />
          <div>
            {isAr
              ? 'يتم تطبيق هذه الإعدادات مباشرة وسحابياً على خوادم تيليجرام الرسمية وتنعكس على كافة أجهزتك المتصلة في نفس اللحظة.'
              : 'These settings are applied directly to official Telegram servers and instantly reflect across all your devices.'}
          </div>
        </div>

        {/* Privacy Dimensions Cards */}
        {PRIVACY_FIELDS.map((field) => {
          const FieldIcon = field.icon;
          const currentValue = getOptionValue(field.id);
          const isFieldUpdating = updatingKey === field.id;

          return (
            <div
              key={field.id}
              className={`p-4 rounded-2xl border transition-all ${
                isDark ? 'bg-[#1e2a38]/40 border-gray-800/80' : 'bg-gray-50/70 border-gray-200/80'
              }`}
            >
              {/* Field Header */}
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-xl bg-[#3390ec]/15 text-[#3390ec]">
                    <FieldIcon className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-sm">
                      {isAr ? field.titleAr : field.titleEn}
                    </h3>
                    <p className="text-xs text-gray-400 mt-0.5">
                      {isAr ? field.descAr : field.descEn}
                    </p>
                  </div>
                </div>

                {isFieldUpdating && (
                  <span className="flex items-center gap-1 text-[11px] text-[#3390ec] font-medium bg-[#3390ec]/10 px-2 py-0.5 rounded-full">
                    <RefreshCw className="w-3 h-3 animate-spin" />
                    <span>{isAr ? 'مزامنة...' : 'Syncing...'}</span>
                  </span>
                )}
              </div>

              {/* Selector Buttons */}
              <div className="grid grid-cols-3 gap-2 pt-1">
                {options.map((opt) => {
                  const isSelected = currentValue === opt.id;
                  const OptIcon = opt.icon;

                  return (
                    <button
                      key={opt.id}
                      type="button"
                      disabled={isFieldUpdating}
                      onClick={() => handleUpdatePrivacy(field.id, opt.id)}
                      className={`py-2.5 px-2 rounded-xl text-xs font-medium border flex flex-col items-center justify-center gap-1.5 transition-all ${
                        isSelected
                          ? 'bg-[#3390ec] text-white border-[#3390ec] shadow-sm shadow-[#3390ec]/30 font-semibold'
                          : isDark
                          ? 'bg-[#17212b] border-gray-700/60 text-gray-300 hover:bg-[#232e3c] hover:border-gray-600'
                          : 'bg-white border-gray-200 text-gray-700 hover:bg-gray-100 hover:border-gray-300'
                      }`}
                    >
                      <OptIcon className="w-4 h-4 shrink-0" />
                      <span className="truncate max-w-full">
                        {isAr ? opt.labelAr : opt.labelEn}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default PrivacySettings;
