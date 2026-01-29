import JSZip from 'jszip';
import { Ticket, WorkflowStep } from '../types';

export class HtmlExportService {
  static generateTicketHtml(ticket: Ticket, steps: WorkflowStep[]): string {
    const statusColor = {
      OPEN: '#3B82F6',
      WIP: '#F59E0B',
      COMPLETED: '#10B981',
      CANCELLED: '#EF4444',
    }[ticket.status] || '#6B7280';

    const priorityColor = {
      low: '#10B981',
      medium: '#F59E0B',
      high: '#EF4444',
      critical: '#DC2626',
    }[ticket.priority] || '#6B7280';

    const stepsList = steps
      .map(
        (step) => `
      <div style="margin-bottom: 20px; padding: 15px; background: #F9FAFB; border-left: 4px solid ${statusColor}; border-radius: 4px;">
        <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 10px;">
          <h3 style="margin: 0; color: #1F2937; font-size: 16px;">
            ${step.stepNumber}. ${step.title}
          </h3>
          <span style="padding: 4px 12px; background: ${statusColor}; color: white; border-radius: 12px; font-size: 12px; font-weight: 500;">
            ${step.status}
          </span>
        </div>
        ${step.description ? `<p style="margin: 10px 0; color: #6B7280;">${step.description}</p>` : ''}
        <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin-top: 10px; font-size: 14px;">
          ${step.assignedTo ? `<div><strong>Assigned To:</strong> ${step.assignedTo}</div>` : ''}
          ${step.progress !== undefined ? `<div><strong>Progress:</strong> ${step.progress}%</div>` : ''}
          ${step.dueDate ? `<div><strong>Due Date:</strong> ${new Date(step.dueDate).toLocaleDateString()}</div>` : ''}
          ${step.completedAt ? `<div><strong>Completed:</strong> ${new Date(step.completedAt).toLocaleDateString()}</div>` : ''}
        </div>
      </div>
    `
      )
      .join('');

    return `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Ticket ${ticket.ticketNumber}</title>
  <style>
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      line-height: 1.6;
      color: #1F2937;
      max-width: 1200px;
      margin: 0 auto;
      padding: 20px;
      background: #F3F4F6;
    }
    .container {
      background: white;
      padding: 30px;
      border-radius: 8px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }
    .header {
      border-bottom: 2px solid #E5E7EB;
      padding-bottom: 20px;
      margin-bottom: 30px;
    }
    .title {
      font-size: 28px;
      font-weight: bold;
      color: #111827;
      margin-bottom: 10px;
    }
    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 15px;
      margin: 20px 0;
    }
    .info-item {
      padding: 10px;
      background: #F9FAFB;
      border-radius: 4px;
    }
    .info-label {
      font-size: 12px;
      color: #6B7280;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 5px;
    }
    .info-value {
      font-size: 14px;
      font-weight: 500;
      color: #111827;
    }
    .section-title {
      font-size: 20px;
      font-weight: 600;
      color: #111827;
      margin: 30px 0 15px 0;
      padding-bottom: 10px;
      border-bottom: 1px solid #E5E7EB;
    }
    @media print {
      body { background: white; }
      .container { box-shadow: none; }
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <div class="title">Ticket ${ticket.ticketNumber}</div>
      <h2 style="color: #4B5563; margin: 0;">${ticket.title}</h2>
    </div>

    <div class="info-grid">
      <div class="info-item">
        <div class="info-label">Status</div>
        <div class="info-value" style="color: ${statusColor};">${ticket.status}</div>
      </div>
      <div class="info-item">
        <div class="info-label">Priority</div>
        <div class="info-value" style="color: ${priorityColor};">${ticket.priority}</div>
      </div>
      ${ticket.createdBy ? `
      <div class="info-item">
        <div class="info-label">Created By</div>
        <div class="info-value">${ticket.createdBy}</div>
      </div>` : ''}
      ${ticket.assignedTo ? `
      <div class="info-item">
        <div class="info-label">Assigned To</div>
        <div class="info-value">${ticket.assignedTo}</div>
      </div>` : ''}
      ${ticket.dueDate ? `
      <div class="info-item">
        <div class="info-label">Due Date</div>
        <div class="info-value">${new Date(ticket.dueDate).toLocaleDateString()}</div>
      </div>` : ''}
      ${ticket.createdAt ? `
      <div class="info-item">
        <div class="info-label">Created</div>
        <div class="info-value">${new Date(ticket.createdAt).toLocaleDateString()}</div>
      </div>` : ''}
    </div>

    ${ticket.description ? `
    <div class="section-title">Description</div>
    <p style="color: #4B5563; margin: 0 0 20px 0;">${ticket.description}</p>
    ` : ''}

    ${steps.length > 0 ? `
    <div class="section-title">Workflow Steps (${steps.length})</div>
    ${stepsList}
    ` : ''}

    <div style="margin-top: 40px; padding-top: 20px; border-top: 1px solid #E5E7EB; text-align: center; color: #9CA3AF; font-size: 12px;">
      Generated on ${new Date().toLocaleString()}
    </div>
  </div>
</body>
</html>
    `.trim();
  }

  static async exportTicketAsZip(ticket: Ticket, steps: WorkflowStep[]): Promise<void> {
    const zip = new JSZip();
    const html = this.generateTicketHtml(ticket, steps);
    zip.file(`ticket-${ticket.ticketNumber}.html`, html);

    const blob = await zip.generateAsync({ type: 'blob' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `ticket-${ticket.ticketNumber}.zip`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }
}

export const htmlExportService = new HtmlExportService();
