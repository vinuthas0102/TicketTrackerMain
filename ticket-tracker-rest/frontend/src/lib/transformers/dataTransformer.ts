export const safeParseDate = (value: any): Date | undefined => {
  if (!value) return undefined;

  if (value instanceof Date) {
    return isNaN(value.getTime()) ? undefined : value;
  }

  if (typeof value === 'string' || typeof value === 'number') {
    const date = new Date(value);
    return isNaN(date.getTime()) ? undefined : date;
  }

  return undefined;
};

export const safeDateToISOString = (date: Date | string | undefined | null): string => {
  if (!date) return '';

  try {
    const parsedDate = safeParseDate(date);
    if (!parsedDate) return '';
    return parsedDate.toISOString().split('T')[0];
  } catch (error) {
    return '';
  }
};

export const getCurrentDateISOString = (): string => {
  return new Date().toISOString().split('T')[0];
};

export const roleToBackend = (frontendRole: string): string => {
  const mapping: Record<string, string> = {
    'EO': 'eo',
    'DO': 'dept_officer',
    'EMPLOYEE': 'employee',
    'VENDOR': 'vendor',
    'FINANCE': 'finance',
    'TECHNICIAN': 'technician',
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
    'technician': 'TECHNICIAN',
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
    lastLogin: safeParseDate(user.last_login),
    lockedUntil: safeParseDate(user.locked_until),
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
    request_type: ticket.requestType,
    data: {
      ...(ticket.data || {}),
      category: ticket.category || 'General',
      department: ticket.department || '',
    },
  };
};

export const transformTicketFromBackend = (ticket: any): any => {
  const createdAt = safeParseDate(ticket.createdAt);
  const updatedAt = safeParseDate(ticket.updatedAt);

  return {
    id: ticket.id,
    ticketNumber: ticket.ticketNumber,
    moduleId: ticket.moduleId,
    title: ticket.title,
    description: ticket.description || '',
    status: statusToFrontend(ticket.status),
    priority: ticket.priority,
    createdBy: ticket.createdBy,
    assignedTo: ticket.assignedTo,
    createdAt: createdAt || new Date(),
    updatedAt: updatedAt || new Date(),
    dueDate: safeParseDate(ticket.dueDate),
    startDate: safeParseDate(ticket.startDate),
    department: ticket.data?.department || '',
    category: ticket.data?.category || ticket.category || 'General',
    propertyId: ticket.propertyId || 'PROP001',
    propertyLocation: ticket.propertyLocation || 'Location01',
    completionDocumentsRequired: ticket.completionDocumentsRequired !== false,
    requiresFinanceApproval: ticket.requiresFinanceApproval !== false,
    financeOfficerId: ticket.financeOfficerId,
    financeSubmissionCount: ticket.financeSubmissionCount || 0,
    latestFinanceStatus: ticket.latestFinanceStatus,
    requestType: ticket.requestType || ticket.request_type,
    workflow: ticket.workflow || [],
    attachments: ticket.attachments || [],
    auditTrail: ticket.auditLog || [],
  };
};

export const transformWorkflowStepFromBackend = (step: any): any => {
  const createdAt = safeParseDate(step.created_at);

  return {
    id: step.id,
    ticketId: step.ticketId,
    stepNumber: step.step_number || step.stepNumber || 1,
    title: step.title,
    description: step.description || '',
    status: statusToFrontend(step.status),
    assignedTo: step.assignedTo,
    createdBy: step.createdBy,
    createdAt: createdAt || new Date(),
    completedAt: safeParseDate(step.completedAt),
    dueDate: safeParseDate(step.dueDate),
    startDate: safeParseDate(step.startDate),
    level_1: step.level_1 || 0,
    level_2: step.level_2 || 0,
    level_3: step.level_3 || 0,
    parentStepId: step.parentStepId,
    is_parallel: step.isParallel !== false,
    progress: step.progress || 0,
    dependencies: step.dependencies || [],
    dependency_mode: step.dependencyMode || 'all',
    is_dependency_locked: step.isDependencyLocked || false,
    mandatory_documents: step.mandatoryDocuments || [],
    optional_documents: step.optionalDocuments || [],
    completionCertificateRequired: step.completion_certificate_required || step.completionCertificateRequired || false,
    stepType: step.stepType || step.step_type,
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
    completion_certificate_required: step.completionCertificateRequired || false,
    step_type: step.stepType || step.step_type,
  };
};
