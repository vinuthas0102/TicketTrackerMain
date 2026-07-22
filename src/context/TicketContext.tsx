import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { Ticket, TicketStatus, StatusTransitionRequest, WorkflowStep, User, Module, BulkStepInput, BulkOperationResult, BulkTicketInput, BulkTicketOperationResult } from '../types';
import { TicketService } from '../services/ticketService';
import { AuthService } from '../services/authService';
import { useAuth } from './AuthContext';

interface TicketContextType {
  tickets: Ticket[];
  users: User[];
  loading: boolean;
  error: string | null;
  bulkOperationInProgress: boolean;
  createTicket: (ticket: Omit<Ticket, 'id' | 'ticketNumber' | 'createdAt' | 'updatedAt' | 'workflow' | 'attachments' | 'auditTrail'>, copiedFromTicketId?: string) => Promise<void>;
  createTicketsBulk: (tickets: BulkTicketInput[]) => Promise<BulkTicketOperationResult>;
  updateTicket: (id: string, updates: Partial<Ticket>) => Promise<void>;
  changeTicketStatus: (request: StatusTransitionRequest) => Promise<void>;
  deleteTicket: (id: string) => Promise<void>;
  getTicketById: (id: string) => Ticket | undefined;
  getFilteredTickets: (filters: TicketFilters) => Ticket[];
  addStep: (ticketId: string, step: Omit<WorkflowStep, 'id' | 'createdAt' | 'comments' | 'attachments'>) => Promise<void>;
  updateStep: (ticketId: string, stepId: string, updates: Partial<WorkflowStep>, remarks?: string) => Promise<void>;
  deleteStep: (ticketId: string, stepId: string) => Promise<void>;
  addStepsBulk: (ticketId: string, steps: BulkStepInput[], parentStepId?: string) => Promise<BulkOperationResult>;
}

interface TicketFilters {
  search?: string;
  status?: TicketStatus;
  assignedTo?: string;
  department?: string;
  priority?: string;
  createdBy?: string;
}

const TicketContext = createContext<TicketContextType | undefined>(undefined);

export const useTickets = () => {
  const context = useContext(TicketContext);
  if (context === undefined) {
    throw new Error('useTickets must be used within a TicketProvider');
  }
  return context;
};

interface TicketProviderProps {
  children: ReactNode;
}

export const TicketProvider: React.FC<TicketProviderProps> = ({ children }) => {
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [bulkOperationInProgress, setBulkOperationInProgress] = useState(false);
  const { user, selectedModule } = useAuth();

  useEffect(() => {
    const loadTickets = async () => {
      try {
        setLoading(true);
        setError(null);

        console.log('TicketContext: Loading data for user:', user?.id, 'role:', user?.role, 'module:', selectedModule?.id);

        const usersData = await AuthService.getAllUsers();
        setUsers(usersData);
        console.log('TicketContext: Loaded users:', usersData.length);

        if (selectedModule && user) {
          console.log('TicketContext: Fetching tickets for module:', selectedModule.name);
          const ticketsData = await TicketService.getTicketsByModule(
            selectedModule.id,
            user.id,
            user.role
          );
          console.log('TicketContext: Loaded tickets:', ticketsData.length);
          if (ticketsData.length > 0) {
            console.log('TicketContext: Sample ticket data:', {
              id: ticketsData[0].id,
              ticketNumber: ticketsData[0].ticketNumber,
              title: ticketsData[0].title,
              category: ticketsData[0].category,
              department: ticketsData[0].department,
              hasCategory: ticketsData[0].category !== undefined,
              hasDepartment: ticketsData[0].department !== undefined
            });
          }
          setTickets(ticketsData);
        } else {
          console.log('TicketContext: No module or user selected, clearing tickets');
          setTickets([]);
        }
      } catch (err) {
        console.error('TicketContext: Failed to load tickets:', {
          error: err,
          message: err instanceof Error ? err.message : 'Unknown error',
          user: user?.id,
          role: user?.role,
          module: selectedModule?.id
        });

        const errorMessage = err instanceof Error ? err.message : 'Failed to load tickets';
        setError(errorMessage);
        setTickets([]);
      } finally {
        setLoading(false);
      }
    };

    loadTickets();
  }, [selectedModule, user]);

  const createTicket = async (ticketData: Omit<Ticket, 'id' | 'ticketNumber' | 'createdAt' | 'updatedAt' | 'workflow' | 'attachments' | 'auditTrail'>, copiedFromTicketId?: string): Promise<string> => {
    try {
      if (!selectedModule) {
        throw new Error('No module selected');
      }

      const effectiveTicketData = user?.role === 'EO' && ticketData.status === 'SUBMITTED'
        ? { ...ticketData, status: 'ACTIVE' as const }
        : ticketData;

      const ticketId = await TicketService.createTicket(effectiveTicketData, copiedFromTicketId);

      // Reload tickets to get the updated list
      if (user) {
        const updatedTickets = await TicketService.getTicketsByModule(
          selectedModule.id,
          user.id,
          user.role
        );
        setTickets(updatedTickets);
      }

      return ticketId;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create ticket');
      throw err;
    }
  };

  const createTicketsBulk = async (ticketsData: BulkTicketInput[]): Promise<BulkTicketOperationResult> => {
    try {
      if (!user) {
        throw new Error('User not authenticated');
      }
      if (!selectedModule) {
        throw new Error('No module selected');
      }

      setBulkOperationInProgress(true);

      const effectiveTicketsData = user.role === 'EO'
        ? ticketsData.map(t => t.status === 'SUBMITTED' ? { ...t, status: 'ACTIVE' as const } : t)
        : ticketsData;

      const result = await TicketService.createTicketsBulk(
        effectiveTicketsData,
        selectedModule.id,
        user.id
      );

      if (user) {
        const updatedTickets = await TicketService.getTicketsByModule(
          selectedModule.id,
          user.id,
          user.role
        );
        setTickets(updatedTickets);
      }

      return result;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create tickets in bulk');
      throw err;
    } finally {
      setBulkOperationInProgress(false);
    }
  };

  const updateTicket = async (id: string, updates: Partial<Ticket>) => {
    try {
      if (!user) throw new Error('User not authenticated');
      if (!selectedModule) throw new Error('No module selected');
      
      const effectiveUpdates = user.role === 'EO' && updates.status === 'SUBMITTED'
        ? { ...updates, status: 'ACTIVE' as const }
        : updates;

      await TicketService.updateTicket(id, effectiveUpdates, user.id);

      // Reload tickets to get the updated list
      const updatedTickets = await TicketService.getTicketsByModule(
        selectedModule.id,
        user.id,
        user.role
      );
      setTickets(updatedTickets);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update ticket');
      throw err;
    }
  };

  const changeTicketStatus = async (request: StatusTransitionRequest) => {
    try {
      if (!user) throw new Error('User not authenticated');
      if (!selectedModule) throw new Error('No module selected');
      
      console.log('TicketContext.changeTicketStatus called:', request);
      
      await TicketService.changeTicketStatus(request, user.id);

      // Reload tickets to get the updated list
      const updatedTickets = await TicketService.getTicketsByModule(
        selectedModule.id,
        user.id,
        user.role
      );
      setTickets(updatedTickets);
      
      // Clear any previous errors
      setError(null);
      
      console.log('Status change completed successfully');
    } catch (err) {
      console.error('Status change error in context:', err);
      setError(err instanceof Error ? err.message : 'Failed to change status');
      throw err;
    }
  };

  const deleteTicket = async (id: string) => {
    try {
      if (!selectedModule) throw new Error('No module selected');
      if (!user) throw new Error('Not authenticated');

      await TicketService.deleteTicket(id, user.id);

      // Reload tickets to get the updated list
      if (user) {
        const updatedTickets = await TicketService.getTicketsByModule(
          selectedModule.id,
          user.id,
          user.role
        );
        setTickets(updatedTickets);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete ticket');
      throw err;
    }
  };

  const getTicketById = (id: string): Ticket | undefined => {
    return tickets.find(ticket => ticket.id === id);
  };

  const getFilteredTickets = (filters: TicketFilters): Ticket[] => {
    let filtered = tickets;

    // Apply search filter
    if (filters.search) {
      const searchLower = filters.search.toLowerCase();
      filtered = filtered.filter(ticket =>
        ticket.id.toLowerCase().includes(searchLower) ||
        ticket.title.toLowerCase().includes(searchLower) ||
        ticket.description.toLowerCase().includes(searchLower)
      );
    }

    // Apply status filter
    if (filters.status) {
      filtered = filtered.filter(ticket => ticket.status === filters.status);
    }

    // Role-aware filtering for DO and TECHNICIAN:
    // - DRAFT/REVIEWED/SUBMITTED: only show tickets created by this user
    // - ACTIVE/COMPLETED/CANCELLED: only show tickets assigned to this user
    //   (ticket-level or workflow-step-level)
    if (user && (user.role === 'DO' || user.role === 'TECHNICIAN') && filters.status) {
      const creatorStatuses: TicketStatus[] = ['DRAFT', 'REVIEWED', 'SUBMITTED'];
      const assigneeStatuses: TicketStatus[] = ['ACTIVE', 'COMPLETED', 'CANCELLED'];

      if (creatorStatuses.includes(filters.status)) {
        filtered = filtered.filter(ticket => ticket.createdBy === user.id);
      } else if (assigneeStatuses.includes(filters.status)) {
        filtered = filtered.filter(ticket =>
          ticket.assignedTo === user.id ||
          ticket.workflow.some(step => step.assignedTo === user.id)
        );
      }
    }

    // Apply assignee filter
    if (filters.assignedTo) {
      filtered = filtered.filter(ticket => ticket.assignedTo === filters.assignedTo);
    }

    // Apply department filter
    if (filters.department) {
      filtered = filtered.filter(ticket => ticket.department === filters.department);
    }

    // Apply priority filter
    if (filters.priority) {
      filtered = filtered.filter(ticket => ticket.priority === filters.priority);
    }

    return filtered;
  };

  const addStep = async (ticketId: string, stepData: Omit<WorkflowStep, 'id' | 'createdAt' | 'comments' | 'attachments'>) => {
    try {
      if (!user) throw new Error('User not authenticated');
      if (!selectedModule) throw new Error('No module selected');

      console.log('TicketContext.addStep called with:', {
        ticketId,
        stepData,
        userId: user.id
      });

      const stepId = await TicketService.addStep(ticketId, stepData, user.id);

      // Reload tickets to get the updated list
      const updatedTickets = await TicketService.getTicketsByModule(
        selectedModule.id,
        user.id,
        user.role
      );
      setTickets(updatedTickets);

      console.log('Step added and tickets reloaded successfully');

      return stepId;
    } catch (err) {
      console.error('Error in TicketContext.addStep:', err);
      setError(err instanceof Error ? err.message : 'Failed to add step');
      throw err;
    }
  };

  const updateStep = async (ticketId: string, stepId: string, updates: Partial<WorkflowStep>, remarks?: string) => {
    try {
      if (!user) throw new Error('User not authenticated');
      if (!selectedModule) throw new Error('No module selected');

      await TicketService.updateStep(ticketId, stepId, updates, user.id, remarks);

      // Reload tickets to get the updated list
      const updatedTickets = await TicketService.getTicketsByModule(
        selectedModule.id,
        user.id,
        user.role
      );
      setTickets(updatedTickets);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update step');
      throw err;
    }
  };

  const deleteStep = async (ticketId: string, stepId: string) => {
    try {
      if (!user) throw new Error('User not authenticated');
      if (!selectedModule) throw new Error('No module selected');

      await TicketService.deleteStep(stepId, ticketId, user.id);

      // Reload tickets to get the updated list
      const updatedTickets = await TicketService.getTicketsByModule(
        selectedModule.id,
        user.id,
        user.role
      );
      setTickets(updatedTickets);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete step');
      throw err;
    }
  };

  const addStepsBulk = async (
    ticketId: string,
    steps: BulkStepInput[],
    parentStepId?: string
  ): Promise<BulkOperationResult> => {
    try {
      if (!user) throw new Error('User not authenticated');
      if (!selectedModule) throw new Error('No module selected');

      setBulkOperationInProgress(true);

      const result = await TicketService.addStepsBulk(ticketId, steps, user.id, parentStepId);

      const updatedTickets = await TicketService.getTicketsByModule(
        selectedModule.id,
        user.id,
        user.role
      );
      setTickets(updatedTickets);

      return result;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add steps in bulk');
      throw err;
    } finally {
      setBulkOperationInProgress(false);
    }
  };

  const value: TicketContextType = {
    tickets,
    users,
    loading,
    error,
    bulkOperationInProgress,
    createTicket,
    createTicketsBulk,
    updateTicket,
    changeTicketStatus,
    deleteTicket,
    getTicketById,
    getFilteredTickets,
    addStep,
    updateStep,
    deleteStep,
    addStepsBulk,
  };

  return (
    <TicketContext.Provider value={value}>
      {children}
    </TicketContext.Provider>
  );
};