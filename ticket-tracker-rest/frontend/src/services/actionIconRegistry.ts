import { Copy, Trash2, Printer, Download, Edit, RefreshCw, Send } from 'lucide-react';

export interface ActionIcon {
  id: string;
  name: string;
  icon: any;
  action: string;
  tooltip: string;
  color?: string;
  requiresConfirmation?: boolean;
}

export const actionIconRegistry: ActionIcon[] = [
  {
    id: 'copy',
    name: 'Copy Ticket',
    icon: Copy,
    action: 'copy',
    tooltip: 'Create a copy of this ticket',
    color: 'text-blue-600',
  },
  {
    id: 'edit',
    name: 'Edit',
    icon: Edit,
    action: 'edit',
    tooltip: 'Edit ticket details',
    color: 'text-gray-600',
  },
  {
    id: 'delete',
    name: 'Delete',
    icon: Trash2,
    action: 'delete',
    tooltip: 'Delete this ticket',
    color: 'text-red-600',
    requiresConfirmation: true,
  },
  {
    id: 'print',
    name: 'Print',
    icon: Printer,
    action: 'print',
    tooltip: 'Print ticket details',
    color: 'text-gray-600',
  },
  {
    id: 'export',
    name: 'Export',
    icon: Download,
    action: 'export',
    tooltip: 'Export ticket as HTML',
    color: 'text-green-600',
  },
  {
    id: 'refresh',
    name: 'Refresh',
    icon: RefreshCw,
    action: 'refresh',
    tooltip: 'Refresh ticket data',
    color: 'text-blue-600',
  },
  {
    id: 'sendToFinance',
    name: 'Send to Finance',
    icon: Send,
    action: 'sendToFinance',
    tooltip: 'Send ticket for finance approval',
    color: 'text-purple-600',
  },
];
