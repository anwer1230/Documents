import React from 'react';
import { SendMonitorComponent } from '../SendMonitorComponent';
import { WhatsAppSettings, SanitizeMode } from '../../types';

interface SendMonitorTabProps {
  settings?: Partial<WhatsAppSettings>;
  monitoringActive?: boolean;
  stats?: { sent?: number; errors?: number; received?: number; failed?: number; [key: string]: any };
  onSaveSettings?: (updated: Partial<WhatsAppSettings>) => void;
  onSendNow?: (data: { message: string; groups: string; images: any[]; send_to_all: boolean; action?: SanitizeMode }) => void;
  onStartMonitoring?: () => void;
  onStopMonitoring?: () => void;
  onBack?: () => void;
}

export const SendMonitorTab: React.FC<SendMonitorTabProps> = ({ onBack }) => {
  return <SendMonitorComponent onBack={onBack} />;
};
