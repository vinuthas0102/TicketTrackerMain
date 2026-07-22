import React, { useMemo } from 'react';
import { AlertTriangle, Calendar, Check, CheckCircle, Clock, CreditCard as Edit, Eye, FileText, IndianRupee, Play, RotateCcw, User, Users, X, XCircle } from 'lucide-react';
import { Ticket, User as UserType } from '../../types';
import { useAuth } from '../../context/AuthContext';

interface TicketTableProps {
  tickets: Ticket[];
  getUserById: (id: string) => UserType | undefined;
  onTicketClick: (ticket: Ticket) => void;
  onModify: (ticket: Ticket) => void;
  onApprove: (ticket: Ticket) => void;
  onClose: (ticket: Ticket) => void;
  onCancel: (ticket: Ticket) => void;
  onMarkInProgress: (ticket: Ticket) => void;
  onReopen: (ticket: Ticket) => void;
  onReinstate: (ticket: Ticket) => void;
  onSendToFinance: (ticket: Ticket) => void;
}

const getStatusColor = (status: string) => {
  switch (status) {
    case 'DRAFT': return 'bg-slate-100 text-slate-700 border-slate-300';
    case 'SUBMITTED': return 'bg-blue-100 text-blue-700 border-blue-300';
    case 'REVIEWED': return 'bg-teal-100 text-teal-700 border-teal-300';
    case 'CREATED': return 'bg-sky-100 text-sky-700 border-sky-300';
    case 'APPROVED': return 'bg-emerald-100 text-emerald-700 border-emerald-300';
    case 'ACTIVE': return 'bg-amber-100 text-amber-700 border-amber-300';
    case 'SENT_TO_FINANCE': return 'bg-yellow-100 text-yellow-700 border-yellow-300';
    case 'APPROVED_BY_FINANCE': return 'bg-green-100 text-green-700 border-green-300';
    case 'REJECTED_BY_FINANCE': return 'bg-red-100 text-red-700 border-red-300';
    case 'COMPLETED': return 'bg-green-700 text-white border-green-800';
    case 'CLOSED': return 'bg-gray-100 text-gray-700 border-gray-300';
    case 'CANCELLED': return 'bg-rose-100 text-rose-700 border-rose-300';
    default: return 'bg-slate-100 text-slate-700 border-slate-300';
  }
};

const getStatusIcon = (status: string) => {
  switch (status) {
    case 'DRAFT': return <FileText className="w-3 h-3" />;
    case 'SUBMITTED': return <Check className="w-3 h-3" />;
    case 'REVIEWED': return <Eye className="w-3 h-3" />;
    case 'CREATED': return <Clock className="w-3 h-3" />;
    case 'ACTIVE': return <Users className="w-3 h-3" />;
    case 'SENT_TO_FINANCE': return <IndianRupee className="w-3 h-3" />;
    case 'APPROVED_BY_FINANCE': return <CheckCircle className="w-3 h-3" />;
    case 'REJECTED_BY_FINANCE': return <XCircle className="w-3 h-3" />;
    case 'COMPLETED': return <CheckCircle className="w-3 h-3" />;
    case 'CANCELLED': return <XCircle className="w-3 h-3" />;
    default: return <FileText className="w-3 h-3" />;
  }
};

const getPriorityColor = (priority: string) => {
  switch (priority) {
    case 'CRITICAL': return 'text-rose-700 bg-rose-50 border-rose-300';
    case 'HIGH': return 'text-orange-700 bg-orange-50 border-orange-300';
    case 'MEDIUM': return 'text-yellow-700 bg-yellow-50 border-yellow-300';
    case 'LOW': return 'text-emerald-700 bg-emerald-50 border-emerald-300';
    default: return 'text-slate-700 bg-slate-50 border-slate-300';
  }
};

const formatDate = (date: Date) =>
  new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }).format(date);

interface RowActionsProps {
  ticket: Ticket;
  onModify: (t: Ticket) => void;
  onApprove: (t: Ticket) => void;
  onClose: (t: Ticket) => void;
  onCancel: (t: Ticket) => void;
  onMarkInProgress: (t: Ticket) => void;
  onReopen: (t: Ticket) => void;
  onReinstate: (t: Ticket) => void;
  onSendToFinance: (t: Ticket) => void;
  onView: (t: Ticket) => void;
}

const RowActions: React.FC<RowActionsProps> = ({
  ticket, onModify, onApprove, onClose, onCancel,
  onMarkInProgress, onReopen, onReinstate, onSendToFinance, onView
}) => {
  const { user } = useAuth();

  const canModify = useMemo(() => {
    if (!user) return false;
    if (user.role === 'EMPLOYEE') return ticket.status === 'DRAFT' && ticket.createdBy === user.id;
    if (user.role === 'EO') return ['DRAFT', 'CREATED'].includes(ticket.status);
    if (user.role === 'DO') return ['DRAFT', 'CREATED'].includes(ticket.status) && ticket.department === user.department;
    return false;
  }, [ticket, user]);

  const canApprove = useMemo(() => {
    if (!user) return false;
    if (user.role === 'EO') return ticket.status === 'CREATED';
    if (user.role === 'DO') return ticket.status === 'CREATED' && ticket.department === user.department;
    return false;
  }, [ticket, user]);

  const canCloseOrCancel = useMemo(() => {
    if (!user) return false;
    if (user.role === 'EO') return ticket.status === 'ACTIVE';
    if (user.role === 'DO') return ticket.status === 'ACTIVE' && ticket.department === user.department;
    return false;
  }, [ticket, user]);

  const canMarkInProgress = useMemo(() => {
    if (!user) return false;
    if (user.role === 'EO') return ticket.status === 'CREATED';
    if (user.role === 'DO') return ticket.status === 'CREATED' && ticket.department === user.department;
    return false;
  }, [ticket, user]);

  const canReopen = useMemo(() => {
    if (!user) return false;
    if (user.role === 'EO') return ticket.status === 'CLOSED';
    if (user.role === 'DO') return ticket.status === 'CLOSED' && ticket.department === user.department;
    return false;
  }, [ticket, user]);

  const canReinstate = useMemo(() => {
    if (!user) return false;
    if (user.role === 'EO') return ticket.status === 'CANCELLED';
    if (user.role === 'DO') return ticket.status === 'CANCELLED' && ticket.department === user.department;
    return false;
  }, [ticket, user]);

  const canSendToFinance = useMemo(() => {
    if (!user) return false;
    if (user.role !== 'EO' && user.role !== 'DO') return false;
    if (ticket.status !== 'ACTIVE' && ticket.status !== 'REJECTED_BY_FINANCE') return false;
    if (ticket.requiresFinanceApproval === false) return false;
    const completedCount = ticket.workflow.filter(s => s.status === 'COMPLETED').length;
    return ticket.workflow.length === 0 || completedCount === ticket.workflow.length;
  }, [ticket, user]);

  const btn = 'p-1 rounded transition-colors duration-150';

  return (
    <div className="flex items-center gap-0.5" onClick={e => e.stopPropagation()}>
      <button className={`${btn} text-slate-500 hover:text-slate-700 hover:bg-slate-100`} title="View Details" onClick={() => onView(ticket)}>
        <Eye className="w-3.5 h-3.5" />
      </button>
      {canModify && (
        <button className={`${btn} text-blue-500 hover:text-blue-700 hover:bg-blue-50`} title="Modify" onClick={() => onModify(ticket)}>
          <Edit className="w-3.5 h-3.5" />
        </button>
      )}
      {canMarkInProgress && (
        <button className={`${btn} text-orange-500 hover:text-orange-700 hover:bg-orange-50`} title="Mark In Progress" onClick={() => onMarkInProgress(ticket)}>
          <Play className="w-3.5 h-3.5" />
        </button>
      )}
      {canApprove && (
        <button className={`${btn} text-green-500 hover:text-green-700 hover:bg-green-50`} title="Approve" onClick={() => onApprove(ticket)}>
          <Check className="w-3.5 h-3.5" />
        </button>
      )}
      {canCloseOrCancel && (
        <>
          <button className={`${btn} text-gray-500 hover:text-gray-700 hover:bg-gray-100`} title="Close" onClick={() => onClose(ticket)}>
            <CheckCircle className="w-3.5 h-3.5" />
          </button>
          <button className={`${btn} text-red-500 hover:text-red-700 hover:bg-red-50`} title="Cancel" onClick={() => onCancel(ticket)}>
            <X className="w-3.5 h-3.5" />
          </button>
        </>
      )}
      {canReopen && (
        <button className={`${btn} text-blue-500 hover:text-blue-700 hover:bg-blue-50`} title="Reopen" onClick={() => onReopen(ticket)}>
          <RotateCcw className="w-3.5 h-3.5" />
        </button>
      )}
      {canReinstate && (
        <button className={`${btn} text-orange-500 hover:text-orange-700 hover:bg-orange-50`} title="Reinstate" onClick={() => onReinstate(ticket)}>
          <RotateCcw className="w-3.5 h-3.5" />
        </button>
      )}
      {canSendToFinance && (
        <button className={`${btn} text-green-600 hover:text-green-800 hover:bg-green-50`} title="Send to Finance" onClick={() => onSendToFinance(ticket)}>
          <IndianRupee className="w-3.5 h-3.5" />
        </button>
      )}
    </div>
  );
};

const TicketTable: React.FC<TicketTableProps> = ({
  tickets, getUserById, onTicketClick,
  onModify, onApprove, onClose, onCancel,
  onMarkInProgress, onReopen, onReinstate, onSendToFinance,
}) => {
  const { user } = useAuth();

  return (
    <div className="bg-white bg-opacity-90 backdrop-blur-sm rounded-xl shadow-lg border border-white border-opacity-30 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-200">
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">Ticket #</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Title</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">Status</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">Priority</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Dept</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">
                <span className="flex items-center gap-1"><Calendar className="w-3.5 h-3.5" />Due</span>
              </th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">
                <span className="flex items-center gap-1"><User className="w-3.5 h-3.5" />Requestor</span>
              </th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">SAP ID</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider text-right whitespace-nowrap">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {tickets.map((ticket, idx) => {
              const createdByUser = getUserById(ticket.createdBy);
              const isOverdue = ticket.dueDate && new Date() > ticket.dueDate
                && ticket.status !== 'COMPLETED' && ticket.status !== 'CANCELLED';

              return (
                <tr
                  key={ticket.id}
                  className={`cursor-pointer transition-colors duration-100 hover:bg-blue-50 ${
                    idx % 2 === 0 ? 'bg-white' : 'bg-gray-50/40'
                  } ${isOverdue ? 'border-l-2 border-l-rose-500' : ''}`}
                  onClick={() => onTicketClick(ticket)}
                >
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span className="text-xs font-bold text-gray-500 font-mono">{ticket.ticketNumber}</span>
                  </td>
                  <td className="px-4 py-3 max-w-xs">
                    <span className="font-medium text-gray-900 line-clamp-1" title={ticket.title}>{ticket.title}</span>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span className={`inline-flex items-center gap-1 px-2 py-0.5 text-xs font-semibold rounded border ${getStatusColor(ticket.status)}`}>
                      {getStatusIcon(ticket.status)}
                      <span>{ticket.status.replace(/_/g, ' ')}</span>
                    </span>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span className={`inline-flex items-center px-2 py-0.5 text-xs font-semibold rounded border ${getPriorityColor(ticket.priority)}`}>
                      {ticket.priority}
                    </span>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span className="text-gray-600 text-xs">{ticket.department}</span>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span className={`text-xs ${isOverdue ? 'text-rose-600 font-semibold' : 'text-gray-600'}`}>
                      {ticket.dueDate ? formatDate(ticket.dueDate) : formatDate(ticket.createdAt)}
                    </span>
                    {isOverdue && (
                      <span className="ml-1 inline-flex items-center gap-0.5 text-rose-600">
                        <AlertTriangle className="w-3 h-3" />
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span className="flex items-center gap-1 text-xs text-gray-700">
                      <User className="w-3.5 h-3.5 text-gray-400" />
                      {createdByUser?.name || '—'}
                    </span>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span className="text-xs text-gray-600 font-mono">{createdByUser?.sapId || '—'}</span>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-right">
                    <RowActions
                      ticket={ticket}
                      onModify={onModify} onApprove={onApprove} onClose={onClose} onCancel={onCancel}
                      onMarkInProgress={onMarkInProgress} onReopen={onReopen} onReinstate={onReinstate}
                      onSendToFinance={onSendToFinance} onView={onTicketClick}
                    />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <div className="px-4 py-2 bg-gray-50 border-t border-gray-100">
        <span className="text-xs text-gray-500">{tickets.length} ticket{tickets.length !== 1 ? 's' : ''}</span>
      </div>
    </div>
  );
};

export default TicketTable;
