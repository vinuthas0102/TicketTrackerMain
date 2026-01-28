export const roleToBackend = (frontendRole: string): string => {
  const mapping: Record<string, string> = {
    'EO': 'eo',
    'DO': 'dept_officer',
    'EMPLOYEE': 'employee',
    'VENDOR': 'vendor',
    'FINANCE': 'finance',
    'ADMIN': 'admin',
  };
  return mapping[frontendRole] || frontendRole.toLowerCase();
};

export const roleToFrontend = (backendRole: string): string => {
  const mapping: Record<string, string> = {
    'eo': 'EO',
    'dept_officer': 'DO',
    'employee': 'EMPLOYEE',
    'vendor': 'VENDOR',
    'finance': 'FINANCE',
    'admin': 'ADMIN',
  };
  return mapping[backendRole] || backendRole.toUpperCase();
};

export const statusToBackend = (frontendStatus: string): string => {
  return frontendStatus.toLowerCase();
};

export const statusToFrontend = (backendStatus: string): string => {
  return backendStatus.toUpperCase();
};

export const transformUserToBackend = (user: any): any => {
  return {
    id: user.id,
    email: user.email,
    name: user.name,
    role: roleToBackend(user.role),
    department: user.department,
    active: user.active !== undefined ? user.active : true,
    locked_until: user.lockedUntil,
  };
};

export const transformUserFromBackend = (user: any): any => {
  return {
    id: user.id,
    username: user.email.split('@')[0],
    email: user.email,
    name: user.name,
    role: roleToFrontend(user.role),
    department: user.department,
    active: user.active,
    lastLogin: user.last_login ? new Date(user.last_login) : undefined,
    lockedUntil: user.locked_until ? new Date(user.locked_until) : undefined,
  };
};

export const transformTicketToBackend = (ticket: any): any => {
  return {
    id: ticket.id,
    ticket_number: ticket.ticketNumber,
    module_id: ticket.moduleId,
    title: ticket.title,
    description: ticket.description || '',
    status: statusToBackend(ticket.status),
    priority: ticket.priority,
    created_by: ticket.createdBy,
    assigned_to: ticket.assignedTo,
    due_date: ticket.dueDate,
    start_date: ticket.startDate,
    property_id: ticket.propertyId,
    property_location: ticket.propertyLocation,
    completion_documents_required: ticket.completionDocumentsRequired !== false,
    requires_finance_approval: ticket.requiresFinanceApproval !== false,
    finance_officer_id: ticket.financeOfficerId,
    finance_submission_count: ticket.financeSubmissionCount || 0,
    latest_finance_status: ticket.latestFinanceStatus,
    data: {
      ...(ticket.data || {}),
      category: ticket.category || 'General',
      department: ticket.department || '',
    },
  };
};

export const transformTicketFromBackend = (ticket: any): any => {
  return {
    id: ticket.id,
    ticketNumber: ticket.ticket_number,
    moduleId: ticket.module_id,
    title: ticket.title,
    description: ticket.description || '',
    status: statusToFrontend(ticket.status),
    priority: ticket.priority,
    createdBy: ticket.created_by,
    assignedTo: ticket.assigned_to,
    createdAt: new Date(ticket.created_at),
    updatedAt: new Date(ticket.updated_at),
    dueDate: ticket.due_date ? new Date(ticket.due_date) : undefined,
    startDate: ticket.start_date ? new Date(ticket.start_date) : undefined,
    department: ticket.data?.department || '',
    category: ticket.data?.category || ticket.category || 'General',
    propertyId: ticket.property_id || 'PROP001',
    propertyLocation: ticket.property_location || 'Location01',
    completionDocumentsRequired: ticket.completion_documents_required !== false,
    requiresFinanceApproval: ticket.requires_finance_approval !== false,
    financeOfficerId: ticket.finance_officer_id,
    financeSubmissionCount: ticket.finance_submission_count || 0,
    latestFinanceStatus: ticket.latest_finance_status,
    workflow: ticket.workflow || [],
    attachments: ticket.attachments || [],
    auditTrail: ticket.audit_trail || [],
  };
};

export const transformWorkflowStepFromBackend = (step: any): any => {
  return {
    id: step.id,
    ticketId: step.ticket_id,
    stepNumber: parseInt(step.step_number) || 1,
    title: step.title,
    description: step.description || '',
    status: statusToFrontend(step.status),
    assignedTo: step.assigned_to,
    createdBy: step.created_by,
    createdAt: new Date(step.created_at),
    completedAt: step.completed_at ? new Date(step.completed_at) : undefined,
    dueDate: step.due_date ? new Date(step.due_date) : undefined,
    startDate: step.start_date ? new Date(step.start_date) : undefined,
    level_1: step.level_1 || 0,
    level_2: step.level_2 || 0,
    level_3: step.level_3 || 0,
    parentStepId: step.parent_step_id,
    is_parallel: step.is_parallel !== false,
    progress: step.progress || 0,
    dependencies: step.dependencies || [],
    dependency_mode: step.dependency_mode || 'all',
    is_dependency_locked: step.is_dependency_locked || false,
    mandatory_documents: step.mandatory_documents || [],
    optional_documents: step.optional_documents || [],
    completionCertificateRequired: step.completion_certificate_required || false,
    comments: [],
    attachments: [],
  };
};

export const transformWorkflowStepToBackend = (step: any): any => {
  return {
    id: step.id,
    ticket_id: step.ticketId,
    step_number: step.stepNumber,
    title: step.title,
    description: step.description || '',
    status: statusToBackend(step.status),
    assigned_to: step.assignedTo,
    due_date: step.dueDate,
    start_date: step.startDate,
    level_1: step.level_1 || 0,
    level_2: step.level_2 || 0,
    level_3: step.level_3 || 0,
    parent_step_id: step.parentStepId,
    is_parallel: step.is_parallel !== false,
    progress: step.progress || 0,
    dependencies: step.dependencies || [],
    dependency_mode: step.dependency_mode || 'all',
    mandatory_documents: step.mandatory_documents || [],
    optional_documents: step.optional_documents || [],
  };
};
