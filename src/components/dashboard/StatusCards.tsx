import React from 'react';
import { FileText, Play, CheckCircle, XCircle, Send, Eye } from 'lucide-react';
import { TicketStatus } from '../../types';
import { useTickets } from '../../context/TicketContext';

type ActiveSubFilter = 'HOD' | 'TECHNICIAN' | null;

interface StatusCardsProps {
  onStatusFilter: (status: TicketStatus | null) => void;
  activeFilter: TicketStatus | null;
  activeSubFilter: ActiveSubFilter;
  onSubFilter: (sub: ActiveSubFilter) => void;
}

const TECHNICIAN_DEPARTMENTS = ['Civil Manager', 'Electrical Manager'];

const StatusCards: React.FC<StatusCardsProps> = ({ onStatusFilter, activeFilter, activeSubFilter, onSubFilter }) => {
  const { tickets, users } = useTickets();

  const statusConfig = [
    {
      status: 'DRAFT' as TicketStatus,
      label: 'Draft',
      icon: FileText,
      color: 'bg-gradient-to-br from-gray-100 to-gray-200 text-gray-800 border-gray-300',
      hoverColor: 'hover:from-gray-200 hover:to-gray-300 hover:shadow-lg'
    },
    {
      status: 'SUBMITTED' as TicketStatus,
      label: 'Submitted',
      icon: Send,
      color: 'bg-gradient-to-br from-blue-100 to-blue-200 text-blue-800 border-blue-300',
      hoverColor: 'hover:from-blue-200 hover:to-blue-300 hover:shadow-lg'
    },
    {
      status: 'REVIEWED' as TicketStatus,
      label: 'Reviewed',
      icon: Eye,
      color: 'bg-gradient-to-br from-teal-100 to-teal-200 text-teal-800 border-teal-300',
      hoverColor: 'hover:from-teal-200 hover:to-teal-300 hover:shadow-lg'
    },
    {
      status: 'ACTIVE' as TicketStatus,
      label: 'Active',
      icon: Play,
      color: 'bg-gradient-to-br from-orange-100 to-orange-200 text-orange-800 border-orange-300',
      hoverColor: 'hover:from-orange-200 hover:to-orange-300 hover:shadow-lg'
    },
    {
      status: 'COMPLETED' as TicketStatus,
      label: 'Completed',
      icon: CheckCircle,
      color: 'bg-gradient-to-br from-green-100 to-green-200 text-green-800 border-green-300',
      hoverColor: 'hover:from-green-200 hover:to-green-300 hover:shadow-lg'
    },
    {
      status: 'CLOSED' as TicketStatus,
      label: 'Closed',
      icon: XCircle,
      color: 'bg-gradient-to-br from-gray-100 to-gray-200 text-gray-800 border-gray-300',
      hoverColor: 'hover:from-gray-200 hover:to-gray-300 hover:shadow-lg'
    },
    {
      status: 'CANCELLED' as TicketStatus,
      label: 'Cancelled',
      icon: XCircle,
      color: 'bg-gradient-to-br from-red-100 to-red-200 text-red-800 border-red-300',
      hoverColor: 'hover:from-red-200 hover:to-red-300 hover:shadow-lg'
    }
  ];

  const getStatusCount = (status: TicketStatus) => {
    return tickets.filter(ticket => ticket.status === status).length;
  };

  const isHODUser = (userId: string) => {
    const u = users.find(u => u.id === userId);
    return u?.role === 'DO' && !TECHNICIAN_DEPARTMENTS.includes(u.department);
  };

  const isTechnicianUser = (userId: string) => {
    const u = users.find(u => u.id === userId);
    return u?.role === 'DO' && TECHNICIAN_DEPARTMENTS.includes(u.department);
  };

  const activeTickets = tickets.filter(t => t.status === 'ACTIVE');

  const hodCount = activeTickets.filter(ticket => {
    if (ticket.assignedTo && isHODUser(ticket.assignedTo)) return true;
    return ticket.workflow.some(step => step.assignedTo && isHODUser(step.assignedTo));
  }).length;

  const technicianCount = activeTickets.filter(ticket => {
    if (ticket.assignedTo && isTechnicianUser(ticket.assignedTo)) return true;
    return ticket.workflow.some(step => step.assignedTo && isTechnicianUser(step.assignedTo));
  }).length;

  return (
    <div className="grid grid-cols-4 md:grid-cols-7 gap-1 mb-2">
      {statusConfig.map((config) => {
        const count = getStatusCount(config.status);
        const isActive = activeFilter === config.status;
        const IconComponent = config.icon;
        const isActiveStatus = config.status === 'ACTIVE';

        return (
          <div
            key={config.status}
            onClick={() => onStatusFilter(isActive ? null : config.status)}
            className={`
              cursor-pointer border rounded-md transition-all duration-200 transform hover:scale-105
              ${config.color} ${config.hoverColor}
              ${isActive ? 'ring-1 ring-blue-400 ring-opacity-50 shadow-md scale-105' : 'shadow-sm hover:shadow-md'}
              ${isActiveStatus && isActive ? 'flex flex-col p-1.5 gap-1.5' : 'flex items-center justify-center p-1 min-h-[40px]'}
            `}
          >
            <div className="flex items-center justify-center space-x-1">
              <IconComponent className="w-3 h-3 opacity-70 shrink-0" />
              <div className="text-sm font-bold">{count}</div>
              <div className="text-xs font-medium truncate">{config.label}</div>
            </div>

            {isActiveStatus && isActive && (
              <div className="flex gap-1" onClick={e => e.stopPropagation()}>
                <button
                  onClick={() => onSubFilter(activeSubFilter === 'HOD' ? null : 'HOD')}
                  className={`
                    flex-1 flex items-center justify-center gap-1 rounded-full py-0.5 px-1.5
                    text-xs font-semibold transition-all duration-150 border
                    ${activeSubFilter === 'HOD'
                      ? 'bg-amber-500 border-amber-500 text-white shadow-sm'
                      : 'bg-white/60 border-amber-300 text-amber-800 hover:bg-amber-100'}
                  `}
                >
                  <span className="font-bold">{hodCount}</span>
                  <span className="opacity-80">HOD</span>
                </button>
                <button
                  onClick={() => onSubFilter(activeSubFilter === 'TECHNICIAN' ? null : 'TECHNICIAN')}
                  className={`
                    flex-1 flex items-center justify-center gap-1 rounded-full py-0.5 px-1.5
                    text-xs font-semibold transition-all duration-150 border
                    ${activeSubFilter === 'TECHNICIAN'
                      ? 'bg-teal-500 border-teal-500 text-white shadow-sm'
                      : 'bg-white/60 border-teal-300 text-teal-800 hover:bg-teal-100'}
                  `}
                >
                  <span className="font-bold">{technicianCount}</span>
                  <span className="opacity-80">Tech</span>
                </button>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};

export default StatusCards;