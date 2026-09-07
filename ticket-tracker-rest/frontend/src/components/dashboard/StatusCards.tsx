import React from 'react';
import { FileText, Play, CheckCircle, XCircle, Send, Eye, Users, Wrench, Hourglass, Clock, CircleDashed } from 'lucide-react';
import { TicketStatus, User } from '../../types';
import { useTickets } from '../../context/TicketContext';
import { classifyActiveTicket } from '../../lib/utils';

type ActiveSubFilter = 'HOD' | 'TECHNICIAN' | 'AWAITING_COMPLETION' | 'WIP' | 'START_TO_WORK' | null;

interface StatusCardsProps {
  onStatusFilter: (status: TicketStatus | null) => void;
  activeFilter: TicketStatus | null;
  activeSubFilter: ActiveSubFilter;
  onSubFilter: (sub: ActiveSubFilter) => void;
  userRole?: User['role'];
  userId?: string;
  reviewByEORequired?: boolean;
}

const StatusCards: React.FC<StatusCardsProps> = ({ onStatusFilter, activeFilter, activeSubFilter, onSubFilter, userRole, userId, reviewByEORequired = true }) => {
  const { tickets, users } = useTickets();

  const creatorStatuses: TicketStatus[] = reviewByEORequired
    ? ['DRAFT', 'REVIEWED', 'SUBMITTED']
    : ['DRAFT', 'SUBMITTED'];
  const assigneeStatuses: TicketStatus[] = ['ACTIVE', 'COMPLETED', 'CANCELLED'];
  const isRoleAware = userRole === 'DO' || userRole === 'TECHNICIAN';

  const submittedLabel = userRole === 'EMPLOYEE' || userRole === 'DO' || userRole === 'TECHNICIAN'
    ? 'Requests Submitted'
    : 'Requests Received';

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
      label: submittedLabel,
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
      status: 'CANCELLED' as TicketStatus,
      label: 'Cancelled',
      icon: XCircle,
      color: 'bg-gradient-to-br from-red-100 to-red-200 text-red-800 border-red-300',
      hoverColor: 'hover:from-red-200 hover:to-red-300 hover:shadow-lg'
    }
  ];

  const getStatusCount = (status: TicketStatus) => {
    return tickets.filter(ticket => {
      if (ticket.status !== status) return false;
      if (!isRoleAware || !userId) return true;
      if (creatorStatuses.includes(status)) return ticket.createdBy === userId;
      if (assigneeStatuses.includes(status))
        return ticket.assignedTo === userId ||
          ticket.workflow.some(step => step.assignedTo === userId);
      return true;
    }).length;
  };

  const activeTickets = tickets.filter(t => t.status === 'ACTIVE');

  const classifications = activeTickets.map(ticket =>
    classifyActiveTicket(ticket.workflow, users)
  );

  const wipCount = classifications.filter(c => c === 'WIP').length;
  const startToWorkCount = classifications.filter(c => c === 'START_TO_WORK').length;
  const hodCount = classifications.filter(c => c === 'HOD').length;
  const technicianCount = classifications.filter(c => c === 'TECHNICIAN').length;
  const awaitingCompletionCount = classifications.filter(c => c === 'AWAITING_COMPLETION').length;

  const showSubFilters = activeFilter === 'ACTIVE';

  return (
    <div className="flex flex-col gap-2 mb-2">
      <div className={`grid gap-1 ${reviewByEORequired ? 'grid-cols-4 md:grid-cols-6' : 'grid-cols-4 md:grid-cols-5'}`}>
        {statusConfig.filter(config => reviewByEORequired || config.status !== 'REVIEWED').map((config) => {
          const count = getStatusCount(config.status);
          const isActive = activeFilter === config.status;
          const IconComponent = config.icon;

          return (
            <div
              key={config.status}
              onClick={() => onStatusFilter(isActive ? null : config.status)}
              className={`
                cursor-pointer border rounded-md p-1 transition-all duration-200 transform hover:scale-105
                ${config.color} ${config.hoverColor}
                ${isActive ? 'ring-1 ring-blue-400 ring-opacity-50 shadow-md scale-105' : 'shadow-sm hover:shadow-md'}
                min-h-[40px] flex items-center justify-center
              `}
            >
              <div className="flex items-center space-x-1">
                <IconComponent className="w-3 h-3 opacity-70 shrink-0" />
                <div className="text-sm font-bold">{count}</div>
                <div className="text-xs font-medium truncate">{config.label}</div>
              </div>
            </div>
          );
        })}
      </div>

      {showSubFilters && (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-1">
          <button
            onClick={() => onSubFilter(activeSubFilter === 'WIP' ? null : 'WIP')}
            className={`
              cursor-pointer border-l-4 border border-blue-400 rounded-md p-1 min-h-[40px]
              flex items-center justify-center space-x-1 transition-all duration-150 hover:shadow-md
              ${activeSubFilter === 'WIP' ? 'bg-blue-100 ring-1 ring-blue-400 shadow-sm' : 'bg-blue-50 hover:bg-blue-100'}
            `}
          >
            <Clock className="w-3 h-3 text-blue-500 shrink-0" />
            <div className="text-sm font-bold text-blue-700">{wipCount}</div>
            <div className="text-xs font-medium text-blue-700 truncate">WIP</div>
          </button>

          <button
            onClick={() => onSubFilter(activeSubFilter === 'START_TO_WORK' ? null : 'START_TO_WORK')}
            className={`
              cursor-pointer border-l-4 border border-slate-400 rounded-md p-1 min-h-[40px]
              flex items-center justify-center space-x-1 transition-all duration-150 hover:shadow-md
              ${activeSubFilter === 'START_TO_WORK' ? 'bg-slate-100 ring-1 ring-slate-400 shadow-sm' : 'bg-slate-50 hover:bg-slate-100'}
            `}
          >
            <CircleDashed className="w-3 h-3 text-slate-500 shrink-0" />
            <div className="text-sm font-bold text-slate-700">{startToWorkCount}</div>
            <div className="text-xs font-medium text-slate-700 truncate">Work Started</div>
          </button>

          <button
            onClick={() => onSubFilter(activeSubFilter === 'HOD' ? null : 'HOD')}
            className={`
              cursor-pointer border-l-4 border border-amber-400 rounded-md p-1 min-h-[40px]
              flex items-center justify-center space-x-1 transition-all duration-150 hover:shadow-md
              ${activeSubFilter === 'HOD' ? 'bg-amber-100 ring-1 ring-amber-400 shadow-sm' : 'bg-amber-50 hover:bg-amber-100'}
            `}
          >
            <Users className="w-3 h-3 text-amber-500 shrink-0" />
            <div className="text-sm font-bold text-amber-700">{hodCount}</div>
            <div className="text-xs font-medium text-amber-700 truncate">Assigned to HOD</div>
          </button>

          <button
            onClick={() => onSubFilter(activeSubFilter === 'TECHNICIAN' ? null : 'TECHNICIAN')}
            className={`
              cursor-pointer border-l-4 border border-teal-400 rounded-md p-1 min-h-[40px]
              flex items-center justify-center space-x-1 transition-all duration-150 hover:shadow-md
              ${activeSubFilter === 'TECHNICIAN' ? 'bg-teal-100 ring-1 ring-teal-400 shadow-sm' : 'bg-teal-50 hover:bg-teal-100'}
            `}
          >
            <Wrench className="w-3 h-3 text-teal-500 shrink-0" />
            <div className="text-sm font-bold text-teal-700">{technicianCount}</div>
            <div className="text-xs font-medium text-teal-700 truncate">Assigned to Technician</div>
          </button>

          <button
            onClick={() => onSubFilter(activeSubFilter === 'AWAITING_COMPLETION' ? null : 'AWAITING_COMPLETION')}
            className={`
              cursor-pointer border-l-4 border border-violet-400 rounded-md p-1 min-h-[40px]
              flex items-center justify-center space-x-1 transition-all duration-150 hover:shadow-md
              ${activeSubFilter === 'AWAITING_COMPLETION' ? 'bg-violet-100 ring-1 ring-violet-400 shadow-sm' : 'bg-violet-50 hover:bg-violet-100'}
            `}
          >
            <Hourglass className="w-3 h-3 text-violet-500 shrink-0" />
            <div className="text-sm font-bold text-violet-700">{awaitingCompletionCount}</div>
            <div className="text-xs font-medium text-violet-700 truncate">Awaiting Completion</div>
          </button>
        </div>
      )}
    </div>
  );
};

export default StatusCards;
