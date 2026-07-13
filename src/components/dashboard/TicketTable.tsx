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
    case 'CRITICAL': return 'text-rose-700 bg-rose-50 border-rose-400';
    case 'HIGH': return 'text-orange-700 bg-orange-50 border-orange-400';
    case 'MEDIUM': return 'text-yellow-700 bg-yellow-50 border-yellow-400';
    case 'LOW': return 'text-emerald-700 bg-emerald-50 border-emerald-400';
    default: return 'text-slate-700 bg-slate-50 border-slate-400';
  }
};

const getPriorityAccent = (priority: string) => {
  switch (priority) {
    case 'CRITICAL': return 'bg-rose-500';
    case 'HIGH': return 'bg-orange-400';
    case 'MEDIUM': return 'bg-yellow-400';
    case 'LOW': return 'bg-emerald-400';
    default: return 'bg-slate-300';
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

  const btnClass = 'p-1 rounded transition-colors duration-150 disabled:opacity-40';

  return (
    <div className="flex items-center gap-0.5" onClick={e => e.stopPropagation()}>
      <button
        className={`${btnClass} text-slate-500 hover:text-slate-700 hover:bg-slate-100`}
        title="View Details"
        onClick={() => onView(ticket)}
      >
        <Eye className="w-3.5 h-3.5" />
      </button>
      {canModify && (
        <button
          className={`${btnClass} text-blue-500 hover:text-blue-700 hover:bg-blue-50`}
          title="Modify"
          onClick={() => onModify(ticket)}
        >
          <Edit className="w-3.5 h-3.5" />
        </button>
      )}
      {canMarkInProgress && (
        <button
          className={`${btnClass} text-orange-500 hover:text-orange-700 hover:bg-orange-50`}
          title="Mark In Progress"
          onClick={() => onMarkInProgress(ticket)}
        >
          <Play className="w-3.5 h-3.5" />
        </button>
      )}
      {canApprove && (
        <button
          className={`${btnClass} text-green-500 hover:text-green-700 hover:bg-green-50`}
          title="Approve"
          onClick={() => onApprove(ticket)}
        >
          <Check className="w-3.5 h-3.5" />
        </button>
      )}
      {canCloseOrCancel && (
        <>
          <button
            className={`${btnClass} text-gray-500 hover:text-gray-700 hover:bg-gray-100`}
            title="Close"
            onClick={() => onClose(ticket)}
          >
            <CheckCircle className="w-3.5 h-3.5" />
          </button>
          <button
            className={`${btnClass} text-red-500 hover:text-red-700 hover:bg-red-50`}
            title="Cancel"
            onClick={() => onCancel(ticket)}
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </>
      )}
      {canReopen && (
        <button
          className={`${btnClass} text-blue-500 hover:text-blue-700 hover:bg-blue-50`}
          title="Reopen"
          onClick={() => onReopen(ticket)}
        >
          <RotateCcw className="w-3.5 h-3.5" />
        </button>
      )}
      {canReinstate && (
        <button
          className={`${btnClass} text-orange-500 hover:text-orange-700 hover:bg-orange-50`}
          title="Reinstate"
          onClick={() => onReinstate(ticket)}
        >
          <RotateCcw className="w-3.5 h-3.5" />
        </button>
      )}
      {canSendToFinance && (
        <button
          className={`${btnClass} text-green-600 hover:text-green-800 hover:bg-green-50`}
          title="Send to Finance"
          onClick={() => onSendToFinance(ticket)}
        >
          <IndianRupee className="w-3.5 h-3.5" />
        </button>
      )}
    </div>
  );
};

const TicketTable: React.FC<TicketTableProps> = ({
  tickets,
  getUserById,
  onTicketClick,
  onModify,
  onApprove,
  onClose,
  onCancel,
  onMarkInProgress,
  onReopen,
  onReinstate,
  onSendToFinance,
}) => {
  const { user } = useAuth();

  return (
    <div className="bg-white bg-opacity-90 backdrop-blur-sm rounded-xl shadow-lg border border-white border-opacity-30 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-200">
              <th className="px-3 py-2.5 text-left text-xs font-semibold text-gray-600 uppercase tracking-wide w-8"></th>
              <th className="px-3 py-2.5 text-left text-xs font-semibold text-gray-600 uppercase tracking-wide whitespace-nowrap">Ticket #</th>
              <th className="px-3 py-2.5 text-left text-xs font-semibold text-gray-600 uppercase tracking-wide">Title</th>
              <th className="px-3 py-2.5 text-left text-xs font-semibold text-gray-600 uppercase tracking-wide whitespace-nowrap">Status</th>
              <th className="px-3 py-2.5 text-left text-xs font-semibold text-gray-600 uppercase tracking-wide whitespace-nowrap">Priority</th>
              <th className="px-3 py-2.5 text-left text-xs font-semibold text-gray-600 uppercase tracking-wide whitespace-nowrap">Department</th>
              {user?.role !== 'EMPLOYEE' && (
                <th className="px-3 py-2.5 text-left text-xs font-semibold text-gray-600 uppercase tracking-wide whitespace-nowrap">Assigned To</th>
              )}
              <th className="px-3 py-2.5 text-left text-xs font-semibold text-gray-600 uppercase tracking-wide whitespace-nowrap">Created By</th>
              <th className="px-3 py-2.5 text-left text-xs font-semibold text-gray-600 uppercase tracking-wide whitespace-nowrap">Due Date</th>
              {user?.role !== 'EMPLOYEE' && (
                <th className="px-3 py-2.5 text-left text-xs font-semibold text-gray-600 uppercase tracking-wide whitespace-nowrap">Progress</th>
              )}
              <th className="px-3 py-2.5 text-right text-xs font-semibold text-gray-600 uppercase tracking-wide whitespace-nowrap">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {tickets.map((ticket, idx) => {
              const createdByUser = getUserById(ticket.createdBy);
              const assignedToUser = ticket.assignedTo ? getUserById(ticket.assignedTo) : undefined;
              const isOverdue = ticket.dueDate && new Date() > ticket.dueDate && ticket.status !== 'COMPLETED' && ticket.status !== 'CANCELLED';
              const completedWorkflows = ticket.workflow.filter(s => s.status === 'COMPLETED').length;
              const totalWorkflows = ticket.workflow.length;
              const progressPct = totalWorkflows > 0 ? Math.round((completedWorkflows / totalWorkflows) * 100) : 0;

              return (
                <tr
                  key={ticket.id}
                  className={`group cursor-pointer transition-colors duration-100 hover:bg-blue-50 ${
                    idx % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'
                  } ${isOverdue ? 'border-l-2 border-l-rose-500' : ''}`}
                  onClick={() => onTicketClick(ticket)}
                >
                  <td className="px-1 py-2">
                    <div className={`w-1.5 h-8 rounded-full mx-auto ${getPriorityAccent(ticket.priority)}`} />
                  </td>
                  <td className="px-3 py-2 whitespace-nowrap">
                    <span className="text-xs font-bold text-gray-700 font-mono">{ticket.ticketNumber}</span>
                  </td>
                  <td className="px-3 py-2 max-w-xs">
                    <div className="flex flex-col gap-0.5">
                      <span className="font-medium text-gray-900 line-clamp-1 text-sm">{ticket.title}</span>
                      {ticket.requestType && (
                        <span className="text-xs text-gray-400 line-clamp-1">{ticket.requestType}</span>
                      )}
                    </div>
                  </td>
                  <td className="px-3 py-2 whitespace-nowrap">
                    <span className={`inline-flex items-center gap-1 px-2 py-0.5 text-xs font-semibold rounded-md border ${getStatusColor(ticket.status)}`}>
                      {getStatusIcon(ticket.status)}
                      <span>{ticket.status.replace(/_/g, ' ')}</span>
                    </span>
                    {isOverdue && (
                      <span className="ml-1 inline-flex items-center gap-0.5 px-1.5 py-0.5 text-xs font-bold rounded bg-rose-500 text-white">
                        <AlertTriangle className="w-2.5 h-2.5" />
                        <span>Late</span>
                      </span>
                    )}
                  </td>
                  <td className="px-3 py-2 whitespace-nowrap">
                    <span className={`inline-flex items-center px-2 py-0.5 text-xs font-semibold rounded-md border ${getPriorityColor(ticket.priority)}`}>
                      {ticket.priority}
                    </span>
                  </td>
                  <td className="px-3 py-2 whitespace-nowrap">
                    <span className="text-xs text-gray-600">{ticket.department}</span>
                  </td>
                  {user?.role !== 'EMPLOYEE' && (
                    <td className="px-3 py-2 whitespace-nowrap">
                      {assignedToUser ? (
                        <div className="flex items-center gap-1 text-xs text-gray-700">
                          <Users className="w-3 h-3 text-gray-400 flex-shrink-0" />
                          <span className="truncate max-w-24" title={assignedToUser.name}>{assignedToUser.name.split(' ')[0]}</span>
                        </div>
                      ) : (
                        <span className="text-xs text-gray-400 italic">Unassigned</span>
                      )}
                    </td>
                  )}
                  <td className="px-3 py-2 whitespace-nowrap">
                    <div className="flex items-center gap-1 text-xs text-gray-700">
                      <User className="w-3 h-3 text-gray-400 flex-shrink-0" />
                      <span className="truncate max-w-24" title={createdByUser?.name}>
                        {createdByUser?.name?.split(' ')[0] || 'Unknown'}
                      </span>
                    </div>
                  </td>
                  <td className="px-3 py-2 whitespace-nowrap">
                    <div className={`flex items-center gap-1 text-xs font-medium ${isOverdue ? 'text-rose-600' : 'text-gray-600'}`}>
                      <Calendar className="w-3 h-3 flex-shrink-0" />
                      <span>{ticket.dueDate ? formatDate(ticket.dueDate) : formatDate(ticket.createdAt)}</span>
                    </div>
                  </td>
                  {user?.role !== 'EMPLOYEE' && (
                    <td className="px-3 py-2">
                      {totalWorkflows > 0 ? (
                        <div className="flex items-center gap-1.5">
                          <div className="w-16 bg-gray-200 rounded-full h-1.5 overflow-hidden">
                            <div
                              className="h-1.5 rounded-full bg-blue-500 transition-all duration-300"
                              style={{ width: `${progressPct}%` }}
                            />
                          </div>
                          <span className="text-xs text-gray-500 whitespace-nowrap">{completedWorkflows}/{totalWorkflows}</span>
                        </div>
                      ) : (
                        <span className="text-xs text-gray-400">—</span>
                      )}
                    </td>
                  )}
                  <td className="px-3 py-2">
                    <RowActions
                      ticket={ticket}
                      onModify={onModify}
                      onApprove={onApprove}
                      onClose={onClose}
                      onCancel={onCancel}
                      onMarkInProgress={onMarkInProgress}
                      onReopen={onReopen}
                      onReinstate={onReinstate}
                      onSendToFinance={onSendToFinance}
                      onView={onTicketClick}
                    />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <div className="px-4 py-2 bg-gray-50 border-t border-gray-100 flex items-center justify-between">
        <span className="text-xs text-gray-500">{tickets.length} ticket{tickets.length !== 1 ? 's' : ''}</span>
      </div>
    </div>
  );
};

export default TicketTable;
