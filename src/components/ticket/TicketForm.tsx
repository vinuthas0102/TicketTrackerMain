import React, { useState, useEffect } from 'react';
import { X, Upload, Copy, Info, AlertCircle, Save } from 'lucide-react';
import { Ticket } from '../../types';
import { useAuth } from '../../context/AuthContext';
import { useTickets } from '../../context/TicketContext';
import { FileService } from '../../services/fileService';
import { MasterDataService } from '../../services/masterDataService';

interface TicketFormProps {
  isOpen: boolean;
  onClose: () => void;
  ticket?: Ticket;
  copiedTicket?: Ticket | null;
  copiedAttachmentIds?: string[];
}

const TicketForm: React.FC<TicketFormProps> = ({ isOpen, onClose, ticket, copiedTicket, copiedAttachmentIds = [] }) => {
  const { user } = useAuth();
  const { selectedModule } = useAuth();
  const { createTicket, updateTicket, tickets } = useTickets();
  const [loading, setLoading] = useState(false);
  const [files, setFiles] = useState<FileList | null>(null);
  const [copyingAttachments, setCopyingAttachments] = useState(false);
  const [attachmentCopyStatus, setAttachmentCopyStatus] = useState<string>('');
  const [pendingStatus, setPendingStatus] = useState<'DRAFT' | 'SUBMITTED'>('SUBMITTED');
  const [validationError, setValidationError] = useState<string | null>(null);
  const [masterCategories, setMasterCategories] = useState<string[]>([]);
  const [masterLocations, setMasterLocations] = useState<string[]>([]);

  useEffect(() => {
    MasterDataService.getActive('categories').then(setMasterCategories).catch(() => setMasterCategories([]));
    MasterDataService.getActive('locations').then(setMasterLocations).catch(() => setMasterLocations([]));
  }, []);
  
  // Get module-specific ticket prefix
  const getTicketPrefix = (moduleId: string): string => {
    const modulePrefixes: Record<string, string> = {
      '550e8400-e29b-41d4-a716-446655440101': 'MTKT', // Maintenance Module
      '550e8400-e29b-41d4-a716-446655440102': 'CTKT', // Complaints Tracker
      '550e8400-e29b-41d4-a716-446655440103': 'GTKT', // Grievances Module
      '550e8400-e29b-41d4-a716-446655440104': 'RTKT', // RTI Tracker
      '550e8400-e29b-41d4-a716-446655440105': 'PTKT'  // Project Execution Plan (PEP)
    };
    return modulePrefixes[moduleId] || 'TKT';
  };

  // Generate ticket number for new tickets
  const generateTicketNumber = () => {
    if (ticket) return ticket.ticketNumber; // Use existing ticket number for editing
    
    const prefix = selectedModule ? getTicketPrefix(selectedModule.id) : 'TKT';
    
    // Get all tickets for the current module and extract their numbers
    const moduleTickets = tickets
      .filter(t => t.moduleId === selectedModule?.id) // Filter by current module
    
    const existingNumbers = moduleTickets.map(t => {
      const match = t.ticketNumber.match(new RegExp(`${prefix}-(\\d+)`));
      return match ? parseInt(match[1]) : 0;
    });
    
    // Find the next available number
    const nextNumber = Math.max(...existingNumbers, 0) + 1;
    return `${prefix}-${String(nextNumber).padStart(3, '0')}`;
  };

  const sourceTicket = ticket || copiedTicket;
  const isCopying = !ticket && !!copiedTicket;

  const [formData, setFormData] = useState({
    ticketNumber: ticket?.ticketNumber || '', // Will be generated on form render
    title: sourceTicket?.title || '',
    description: sourceTicket?.description || '',
    status: ticket?.status || 'DRAFT',
    priority: sourceTicket?.priority || 'MEDIUM',
    category: sourceTicket?.category || 'Civil Maintenance',
    assignedTo: ticket?.assignedTo || '',
    estCompletionDate: ticket?.dueDate ? ticket.dueDate.toISOString().split('T')[0] : '',
    department: user?.department || '',
    propertyId: sourceTicket?.propertyId || 'PROP001',
    propertyLocation: sourceTicket?.propertyLocation || 'Location01',
    requestType: sourceTicket?.requestType || (user?.role === 'EMPLOYEE' ? 'General Maintenance' : '')
  });

  // Update ticket number when component mounts or tickets change
  React.useEffect(() => {
    if (!ticket) {
      setFormData(prev => ({
        ...prev,
        ticketNumber: generateTicketNumber()
      }));
    }
  }, [tickets, selectedModule]);

  // Update form data when copiedTicket changes
  React.useEffect(() => {
    if (copiedTicket && !ticket) {
      setFormData({
        ticketNumber: generateTicketNumber(),
        title: copiedTicket.title || '',
        description: copiedTicket.description || '',
        status: 'DRAFT',
        priority: copiedTicket.priority || 'MEDIUM',
        category: copiedTicket.category || 'Civil Maintenance',
        assignedTo: '',
        estCompletionDate: '',
        department: user?.department || '',
        propertyId: copiedTicket.propertyId || 'PROP001',
        propertyLocation: copiedTicket.propertyLocation || 'Location01',
        requestType: copiedTicket.requestType || (user?.role === 'EMPLOYEE' ? 'General Maintenance' : '')
      });
    }
  }, [copiedTicket]);

  // Sync form data when editing an existing ticket (the ticket prop arrives
  // after first mount, so the useState initializer alone doesn't populate it)
  React.useEffect(() => {
    if (ticket) {
      setFormData({
        ticketNumber: ticket.ticketNumber || '',
        title: ticket.title || '',
        description: ticket.description || '',
        status: ticket.status || 'DRAFT',
        priority: ticket.priority || 'MEDIUM',
        category: ticket.category || 'Civil Maintenance',
        assignedTo: ticket.assignedTo || '',
        estCompletionDate: ticket.dueDate ? ticket.dueDate.toISOString().split('T')[0] : '',
        department: ticket.department || user?.department || '',
        propertyId: ticket.propertyId || 'PROP001',
        propertyLocation: ticket.propertyLocation || 'Location01',
        requestType: ticket.requestType || (user?.role === 'EMPLOYEE' ? 'General Maintenance' : '')
      });
    }
  }, [ticket]);

  const isEditing = !!ticket;
  const isEOEditingOthersTicket = isEditing && user?.role === 'EO' && ticket?.createdBy !== user?.id;

  if (!isOpen) return null;

  const validateForm = (status: 'DRAFT' | 'SUBMITTED'): boolean => {
    setValidationError(null);
    if (!formData.title.trim()) {
      setValidationError('Title is required.');
      return false;
    }
    if (status === 'SUBMITTED') {
      if (!formData.description.trim()) {
        setValidationError('Description is required to submit.');
        return false;
      }
      if (user?.role === 'EMPLOYEE') {
        if (!formData.propertyId) { setValidationError('Property ID is required to submit.'); return false; }
        if (!formData.propertyLocation) { setValidationError('Property Location is required to submit.'); return false; }
      }
      if (availableRequestTypes.length > 0 && !formData.requestType) {
        setValidationError('Request Type is required to submit.'); return false;
      }
    }
    return true;
  };

  const handleSubmit = async (status: 'DRAFT' | 'SUBMITTED', e?: React.FormEvent) => {
    if (e) e.preventDefault();
    
    if (!user || !selectedModule) return;

    if (!validateForm(status)) return;

    setLoading(true);
    try {
      const ticketData = {
        moduleId: selectedModule.id,
        title: formData.title,
        description: formData.description,
        status: status as const,
        priority: formData.priority as const,
        category: formData.category,
        assignedTo: formData.assignedTo || undefined,
        department: formData.department,
        dueDate: formData.estCompletionDate ? new Date(formData.estCompletionDate) : undefined,
        propertyId: formData.propertyId,
        propertyLocation: formData.propertyLocation,
        createdBy: user.id,
        requestType: formData.requestType || undefined,
        requiresFinanceApproval: selectedModule.config?.requiresFinanceApproval ?? false,
      };

      let newTicketId: string | undefined;

      if (isEditing && ticket) {
        await updateTicket(ticket.id, ticketData);
      } else {
        newTicketId = await createTicket(ticketData, copiedTicket?.id);
      }

      if (newTicketId) {
        if (files && files.length > 0) {
          setCopyingAttachments(true);
          setAttachmentCopyStatus(`Uploading ${files.length} file${files.length !== 1 ? 's' : ''}...`);

          try {
            let uploadedCount = 0;
            let failedCount = 0;
            const errors: string[] = [];

            for (let i = 0; i < files.length; i++) {
              try {
                await FileService.uploadStepDocument({
                  file: files[i],
                  ticketId: newTicketId,
                  userId: user.id,
                  isMandatory: false,
                });
                uploadedCount++;
              } catch (error) {
                failedCount++;
                errors.push(`${files[i].name}: ${error instanceof Error ? error.message : 'Unknown error'}`);
              }
            }

            if (uploadedCount > 0) {
              setAttachmentCopyStatus(`Successfully uploaded ${uploadedCount} file${uploadedCount !== 1 ? 's' : ''}`);
            }

            if (failedCount > 0) {
              console.error('File upload errors:', errors);
              alert(
                `Ticket created successfully, but ${failedCount} file${failedCount !== 1 ? 's' : ''} failed to upload:\n${errors.slice(0, 3).join('\n')}`
              );
            }
          } catch (error) {
            console.error('Error uploading files:', error);
            alert('Ticket created successfully, but files failed to upload.');
          } finally {
            setCopyingAttachments(false);
            setTimeout(() => setAttachmentCopyStatus(''), 2000);
          }
        }

        if (copiedTicket && copiedAttachmentIds.length > 0) {
          setCopyingAttachments(true);
          setAttachmentCopyStatus(`Copying ${copiedAttachmentIds.length} attachment${copiedAttachmentIds.length !== 1 ? 's' : ''}...`);

          try {
            const copyResult = await FileService.copyTicketAttachments(
              copiedTicket.id,
              newTicketId,
              user.id,
              copiedAttachmentIds
            );

            if (copyResult.successCount > 0) {
              setAttachmentCopyStatus(
                `Successfully copied ${copyResult.successCount} attachment${copyResult.successCount !== 1 ? 's' : ''}`
              );
            }

            if (copyResult.failedCount > 0) {
              console.error('Attachment copy errors:', copyResult.errors);
              alert(
                `Ticket created successfully, but ${copyResult.failedCount} attachment${copyResult.failedCount !== 1 ? 's' : ''} failed to copy:\n${copyResult.errors.slice(0, 3).join('\n')}`
              );
            }
          } catch (error) {
            console.error('Error copying attachments:', error);
            alert('Ticket created successfully, but attachments failed to copy.');
          } finally {
            setCopyingAttachments(false);
            setTimeout(() => setAttachmentCopyStatus(''), 2000);
          }
        }
      }

      setTimeout(() => {
        onClose();
      }, copyingAttachments ? 2000 : 0);
      // Reset form data
      setPendingStatus('SUBMITTED');
      setValidationError(null);
      setFormData({
        ticketNumber: '', // Will be regenerated by useEffect
        title: '',
        description: '',
        status: 'DRAFT',
        priority: 'MEDIUM',
        category: 'Civil Maintenance',
        assignedTo: '',
        estCompletionDate: '',
        department: user?.department || '',
        propertyId: 'PROP001',
        propertyLocation: 'Location01',
        requestType: user?.role === 'EMPLOYEE' ? 'General Maintenance' : ''
      });
      setFiles(null);
    } catch (error) {
      console.error('Ticket creation error:', error);
      const errorMessage = error instanceof Error ? error.message : 'Failed to save ticket';
      alert(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFiles(e.target.files);
  };

  const availableCategories = masterCategories.length > 0
    ? masterCategories
    : (selectedModule?.config?.categories || [
        'Civil Maintenance',
        'Electrical Maintenance',
        'Plumbing & Sanitary',
        'Carpentry',
        'HVAC / Air Conditioning',
        'Water Supply',
        'Sewage & Drainage',
        'Road & External Area',
        'Housekeeping, Fire & Safety',
        'Security Systems',
        'Street Lighting',
        'Utility Services',
      ]);
  const availableRequestTypes = selectedModule?.config?.requestTypes || [];
  const selectedRequestType = availableRequestTypes.find(rt => rt.value === formData.requestType);
  const showCEInspectionNotice = selectedRequestType?.requiresCEInspection === true;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex min-h-screen items-center justify-center p-4">
        <div className="fixed inset-0 bg-black bg-opacity-50 transition-opacity" onClick={onClose}></div>
        
        <div className="relative bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[95vh] overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-gray-200">
            <div>
              <h2 className="text-lg font-bold text-gray-900">
                {isEditing ? 'Edit Ticket' : isCopying ? 'Create New Ticket from Copy' : 'Create New Ticket'}
              </h2>
              {isCopying && copiedTicket && (
                <div className="mt-2 flex items-center space-x-2 text-sm">
                  <div className="flex items-center space-x-1 text-blue-600">
                    <Copy className="w-4 h-4" />
                    <span className="font-medium">Copied from:</span>
                  </div>
                  <span className="font-mono text-blue-700 font-semibold">{copiedTicket.ticketNumber}</span>
                  <span className="text-gray-500">-</span>
                  <span className="text-gray-700">{copiedTicket.title}</span>
                </div>
              )}
            </div>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 transition-colors duration-200"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Form */}
          <form onSubmit={(e) => handleSubmit('SUBMITTED', e)} className="p-6 max-h-[75vh] overflow-y-auto">
            {validationError && (
              <div className="mb-4 bg-red-50 border border-red-200 rounded-lg p-3">
                <div className="flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
                  <p className="text-sm text-red-700 font-medium">{validationError}</p>
                  <button onClick={() => setValidationError(null)} className="ml-auto text-red-400 hover:text-red-600">
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            )}
            {isCopying && (
              <div className="mb-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
                <div className="flex items-start space-x-3">
                  <Info className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
                  <div className="flex-1">
                    <h3 className="text-sm font-semibold text-blue-900 mb-1">Creating from Copy</h3>
                    <p className="text-xs text-blue-700">
                      The following fields have been pre-filled from the original ticket: Title, Description, Priority, Category, Department, and Property details. You can modify any field before creating the new ticket.
                    </p>
                    {copiedAttachmentIds.length > 0 && (
                      <p className="text-xs text-blue-700 mt-2">
                        <strong>{copiedAttachmentIds.length}</strong> attachment{copiedAttachmentIds.length !== 1 ? 's' : ''} will be copied to the new ticket.
                      </p>
                    )}
                  </div>
                </div>
              </div>
            )}
            {copyingAttachments && (
              <div className="mb-6 bg-green-50 border border-green-200 rounded-lg p-4">
                <div className="flex items-center space-x-3">
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-green-600"></div>
                  <p className="text-sm text-green-800 font-medium">{attachmentCopyStatus}</p>
                </div>
              </div>
            )}
            {!copyingAttachments && attachmentCopyStatus && (
              <div className="mb-6 bg-green-50 border border-green-200 rounded-lg p-4">
                <div className="flex items-center space-x-3">
                  <svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                  </svg>
                  <p className="text-sm text-green-800 font-medium">{attachmentCopyStatus}</p>
                </div>
              </div>
            )}
            <div className="space-y-6">
              {/* Ticket Number and Status - only shown when editing an existing ticket */}
              {isEditing && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Ticket #
                    </label>
                    <input
                      type="text"
                      value={formData.ticketNumber}
                      className="w-full px-2 py-1.5 text-xs border border-gray-300 rounded-md bg-gray-50 text-gray-600"
                      disabled
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Status
                    </label>
                    <input
                      type="text"
                      value={isEditing ? formData.status : pendingStatus}
                      className="w-full px-2 py-1.5 text-xs border border-gray-300 rounded-md bg-gray-50 text-gray-600"
                      disabled
                    />
                  </div>
                </div>
              )}

              {isEOEditingOthersTicket && (
                <div className="mb-4 bg-amber-50 border border-amber-200 rounded-lg p-3">
                  <div className="flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 text-amber-600 flex-shrink-0" />
                    <p className="text-sm text-amber-800 font-medium">
                      You can only modify Priority and Category for tickets raised by other users. All other fields are read-only.
                    </p>
                  </div>
                </div>
              )}

              {/* Title */}
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  Title *
                </label>
                <input
                  type="text"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="Brief description..."
                  disabled={isEOEditingOthersTicket}
                />
              </div>

              {/* Description */}
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  Description *
                </label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  rows={6}
                  className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="Detailed description..."
                  disabled={isEOEditingOthersTicket}
                />
              </div>

              {/* Property ID and Property Location - Only for Employees */}
              {user?.role === 'EMPLOYEE' && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Property ID *
                    </label>
                    <select
                      value={formData.propertyId}
                      onChange={(e) => setFormData({ ...formData, propertyId: e.target.value })}
                      className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    >
                      <option value="PROP001">PROP001</option>
                      <option value="PROP002">PROP002</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">
                      Property Location *
                    </label>
                    <select
                      value={formData.propertyLocation}
                      onChange={(e) => setFormData({ ...formData, propertyLocation: e.target.value })}
                      className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    >
                      {(masterLocations.length > 0 ? masterLocations : ['Location01', 'Location02']).map(loc => (
                        <option key={loc} value={loc}>{loc}</option>
                      ))}
                    </select>
                  </div>
                </div>
              )}

              {/* Priority and Category */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">
                    Priority *
                  </label>
                  <select
                    value={formData.priority}
                    onChange={(e) => setFormData({ ...formData, priority: e.target.value })}
                    className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    required
                  >
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                    <option value="CRITICAL">Critical</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">
                    Category *
                  </label>
                  <select
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                    className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    required
                  >
                    {availableCategories.map(category => (
                      <option key={category} value={category}>{category}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">
                    Est Completion Date
                  </label>
                  <input
                    type="date"
                    value={formData.estCompletionDate}
                    onChange={(e) => setFormData({ ...formData, estCompletionDate: e.target.value })}
                    className="w-full px-2 py-1.5 text-xs border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    min={new Date().toISOString().split('T')[0]}
                    disabled={isEOEditingOthersTicket}
                  />
                </div>
              </div>

              {/* Request Type - hidden from EMPLOYEE role */}
              {availableRequestTypes.length > 0 && user?.role !== 'EMPLOYEE' && (
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">
                    Request Type *
                  </label>
                  <select
                    value={formData.requestType}
                    onChange={(e) => setFormData({ ...formData, requestType: e.target.value })}
                    className="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    disabled={isEOEditingOthersTicket}
                  >
                    <option value="">Select a request type...</option>
                    {availableRequestTypes.map(rt => (
                      <option key={rt.value} value={rt.value}>{rt.label}</option>
                    ))}
                  </select>
                  {showCEInspectionNotice && (
                    <div className="mt-2 flex items-start space-x-2 p-2.5 bg-amber-50 border border-amber-200 rounded-md">
                      <AlertCircle className="w-4 h-4 text-amber-600 flex-shrink-0 mt-0.5" />
                      <p className="text-xs text-amber-800">
                        Civil and Electrical inspection steps will be automatically created for this request type and assigned to their respective managers.
                      </p>
                    </div>
                  )}
                </div>
              )}

              {/* File Upload */}
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  Attachments
                </label>
                <div className={`mt-1 flex justify-center px-4 pt-3 pb-4 border-2 border-gray-300 border-dashed rounded-md ${isEOEditingOthersTicket ? 'opacity-50 pointer-events-none' : ''}`}>
                  <div className="space-y-1 text-center">
                    <Upload className="mx-auto h-8 w-8 text-gray-400" />
                    <div className="flex text-xs text-gray-600">
                      <label htmlFor="file-upload" className="relative cursor-pointer bg-white rounded-md font-medium text-blue-600 hover:text-blue-500 text-xs">
                        <span>Upload files</span>
                        <input
                          id="file-upload"
                          name="file-upload"
                          type="file"
                          className="sr-only"
                          multiple
                          onChange={handleFileChange}
                          accept=".pdf,.doc,.docx,.txt,.jpg,.jpeg,.png,.gif,.xlsx,.xls"
                          disabled={isEOEditingOthersTicket}
                        />
                      </label>
                      <p className="pl-1">or drag and drop</p>
                    </div>
                    <p className="text-xs text-gray-500 px-2">
                      PDF, Word, Excel, Images up to 5MB
                    </p>
                  </div>
                </div>
                
                {files && files.length > 0 && (
                  <div className="mt-2">
                    <p className="text-xs text-gray-600">Selected files:</p>
                    <ul className="text-xs text-gray-500">
                      {Array.from(files).map((file, index) => (
                        <li key={index}>{file.name} ({(file.size / 1024).toFixed(1)} KB)</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            </div>

            {/* Form Actions */}
            <div className="flex justify-end space-x-2 mt-6 pt-4 border-t border-gray-200">
              <button
                type="button"
                onClick={onClose}
                className="px-3 py-1.5 text-sm text-gray-700 bg-gray-200 rounded-md hover:bg-gray-300 transition-colors duration-200"
                disabled={loading}
              >
                Cancel
              </button>
              {(!isEditing || (isEditing && ticket?.status === 'DRAFT')) && (
                <button
                  type="button"
                  onClick={() => { setPendingStatus('DRAFT'); handleSubmit('DRAFT'); }}
                  disabled={loading}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm text-green-700 bg-green-50 border border-green-300 rounded-md hover:bg-green-100 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Save className="w-3.5 h-3.5" />
                  {loading && pendingStatus === 'DRAFT' ? 'Saving...' : isEditing ? 'Save' : 'Save Draft'}
                </button>
              )}
              <button
                type="submit"
                onClick={() => setPendingStatus('SUBMITTED')}
                disabled={loading}
                className="px-3 py-1.5 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loading && pendingStatus === 'SUBMITTED' ? 'Submitting...' : isEditing ? (ticket?.status === 'DRAFT' ? 'Submit Ticket' : 'Update Ticket') : 'Submit Ticket'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default TicketForm;