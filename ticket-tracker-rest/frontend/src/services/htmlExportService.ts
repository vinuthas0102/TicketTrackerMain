import JSZip from 'jszip';

export interface ExportOptions {
  filename?: string;
  includeTimestamp?: boolean;
  pageTitle?: string;
}

class HTMLExportService {
  private preserveElementState(element: HTMLElement): HTMLElement {
    const clonedElement = element.cloneNode(true) as HTMLElement;
    const allElements = [clonedElement, ...Array.from(clonedElement.getElementsByTagName('*'))] as HTMLElement[];

    allElements.forEach((el) => {
      if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.tagName === 'SELECT') {
        const inputEl = el as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement;
        const originalEl = element.querySelector(`[name="${inputEl.name}"]`) as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement;

        if (originalEl) {
          if (inputEl.tagName === 'INPUT' && (inputEl as HTMLInputElement).type === 'checkbox') {
            (inputEl as HTMLInputElement).checked = (originalEl as HTMLInputElement).checked;
            if ((originalEl as HTMLInputElement).checked) {
              inputEl.setAttribute('checked', 'checked');
            }
          } else if (inputEl.tagName === 'INPUT' && (inputEl as HTMLInputElement).type === 'radio') {
            (inputEl as HTMLInputElement).checked = (originalEl as HTMLInputElement).checked;
            if ((originalEl as HTMLInputElement).checked) {
              inputEl.setAttribute('checked', 'checked');
            }
          } else if (inputEl.tagName === 'TEXTAREA') {
            (inputEl as HTMLTextAreaElement).value = (originalEl as HTMLTextAreaElement).value;
            (inputEl as HTMLTextAreaElement).textContent = (originalEl as HTMLTextAreaElement).value;
          } else if (inputEl.tagName === 'SELECT') {
            (inputEl as HTMLSelectElement).value = (originalEl as HTMLSelectElement).value;
            const options = (inputEl as HTMLSelectElement).options;
            for (let i = 0; i < options.length; i++) {
              if (options[i].value === (originalEl as HTMLSelectElement).value) {
                options[i].setAttribute('selected', 'selected');
              }
            }
          } else {
            (inputEl as HTMLInputElement).value = (originalEl as HTMLInputElement).value;
            inputEl.setAttribute('value', (originalEl as HTMLInputElement).value);
          }
        }
      }
    });

    return clonedElement;
  }

  private async convertImagesToBase64(element: HTMLElement): Promise<void> {
    const images = Array.from(element.getElementsByTagName('img'));

    for (const img of images) {
      try {
        if (img.src && !img.src.startsWith('data:')) {
          const response = await fetch(img.src);
          const blob = await response.blob();
          const reader = new FileReader();

          await new Promise((resolve) => {
            reader.onloadend = () => {
              if (reader.result) {
                img.src = reader.result as string;
              }
              resolve(null);
            };
            reader.readAsDataURL(blob);
          });
        }
      } catch (error) {
        console.warn('Failed to convert image to base64:', img.src, error);
      }
    }
  }

  private extractAllCSSRules(): string {
    let cssText = '';

    try {
      for (let i = 0; i < document.styleSheets.length; i++) {
        const sheet = document.styleSheets[i];

        try {
          if (sheet.cssRules) {
            for (let j = 0; j < sheet.cssRules.length; j++) {
              cssText += sheet.cssRules[j].cssText + '\n';
            }
          }
        } catch (e) {
          console.warn('Cannot access stylesheet:', sheet.href, e);
        }
      }
    } catch (error) {
      console.warn('Error extracting CSS rules:', error);
    }

    return cssText;
  }

  private generateComprehensiveCSS(extractedCSS: string): string {
    const timestamp = new Date().toLocaleString();

    return `/* Ticket Tracker - Exported Styles */
/* Generated on: ${timestamp} */

*, *::before, *::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html {
  line-height: 1.5;
  -webkit-text-size-adjust: 100%;
  -moz-tab-size: 4;
  tab-size: 4;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol', 'Noto Color Emoji';
}

body {
  margin: 0;
  line-height: inherit;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

${extractedCSS}

::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: linear-gradient(to bottom, #3b82f6, #6366f1);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(to bottom, #2563eb, #4f46e5);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideIn {
  from { opacity: 0; transform: translateX(-20px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes slideInRight {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.9); }
  to { opacity: 1; transform: scale(1); }
}

.animate-fadeIn { animation: fadeIn 0.5s ease-out; }
.animate-slideIn { animation: slideIn 0.3s ease-out; }
.animate-slideInRight { animation: slideInRight 0.3s ease-out; }
.animate-scaleIn { animation: scaleIn 0.2s ease-out; }

.glass {
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.scrollbar-hide::-webkit-scrollbar { display: none; }
.scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }

@media print {
  body { print-color-adjust: exact; -webkit-print-color-adjust: exact; }
  .no-print { display: none !important; }
}

.export-banner {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background: rgba(37, 99, 235, 0.9);
  color: white;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  z-index: 9999;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

button[disabled], button.export-disabled {
  opacity: 0.5 !important;
  cursor: not-allowed !important;
  pointer-events: none !important;
}

.bg-gradient-to-r, .bg-gradient-to-br, .bg-gradient-to-bl, .bg-gradient-to-tr,
.bg-gradient-to-tl, .bg-gradient-to-b, .bg-gradient-to-t, .bg-gradient-to-l {
  background-size: 100% 100%;
  background-repeat: no-repeat;
}

.shadow-sm { box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05); }
.shadow { box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06); }
.shadow-md { box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06); }
.shadow-lg { box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05); }
.shadow-xl { box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04); }
.shadow-2xl { box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); }

.backdrop-blur-sm { backdrop-filter: blur(4px); -webkit-backdrop-filter: blur(4px); }
.backdrop-blur { backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px); }
.backdrop-blur-md { backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); }
.backdrop-blur-lg { backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); }
`;
  }

  private generateEnhancedJS(): string {
    const timestamp = new Date().toLocaleString();

    return `(function() {
  'use strict';

  function addExportBanner() {
    var banner = document.createElement('div');
    banner.className = 'export-banner';
    banner.innerHTML = 'Static Export View';
    banner.title = 'This is a static exported view. Interactive features are disabled.';
    document.body.appendChild(banner);
  }

  function disableInteractiveElements() {
    var buttons = document.querySelectorAll('button');
    buttons.forEach(function(btn) {
      btn.disabled = true;
      btn.classList.add('export-disabled');
      btn.style.cursor = 'not-allowed';
    });

    var links = document.querySelectorAll('a[href="#"], a[href=""], a:not([href])');
    links.forEach(function(link) {
      link.style.pointerEvents = 'none';
      link.style.opacity = '0.6';
      link.style.cursor = 'not-allowed';
    });

    var inputs = document.querySelectorAll('input, textarea, select');
    inputs.forEach(function(input) {
      input.disabled = true;
      input.style.cursor = 'not-allowed';
    });
  }

  function initialize() {
    window.scrollTo(0, 0);
    document.body.offsetHeight;
    disableInteractiveElements();
    addExportBanner();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initialize);
  } else {
    initialize();
  }

  document.addEventListener('submit', function(e) {
    e.preventDefault();
    return false;
  }, true);
})();
`;
  }

  private generateEnhancedHTMLDocument(bodyContent: string, title: string): string {
    const timestamp = new Date().toLocaleString();

    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="color-scheme" content="light">
  <meta name="generator" content="Ticket Tracker HTML Export">
  <meta name="export-date" content="${timestamp}">
  <meta name="description" content="Exported view from Ticket Tracker System">
  <title>${title}</title>
  <link rel="stylesheet" href="styles.css">
  <style>
    html, body { margin: 0; padding: 0; width: 100%; height: 100%; }
    body { font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.5; }
    #root { opacity: 1; }
  </style>
</head>
<body>
  ${bodyContent}
  <script src="script.js"></script>
</body>
</html>`;
  }

  private sanitizeFilename(filename: string): string {
    return filename
      .replace(/[^a-z0-9_\-]/gi, '_')
      .replace(/_+/g, '_')
      .toLowerCase();
  }

  private generateReadme(exportOptions: ExportOptions): string {
    const timestamp = new Date().toLocaleString();

    return `# Ticket Tracker - Exported View

**Export Date:** ${timestamp}
**Export Title:** ${exportOptions.pageTitle || 'Ticket Tracker Export'}

## Contents

- **index.html** - The main HTML document containing the exported view
- **styles.css** - Complete stylesheet including all Tailwind CSS utilities and custom styles
- **script.js** - JavaScript for handling the static export view
- **README.md** - This file

## How to View

1. Extract all files from this ZIP archive to a folder
2. Open \`index.html\` in any modern web browser
3. The page should display with full styling and layout preserved

## Notes

- This is a static export - interactive features are disabled
- All styles are self-contained; no internet connection is required
- Images have been embedded as base64 data URIs

---

Generated by Ticket Tracker System
`;
  }

  async exportCurrentView(options: ExportOptions = {}): Promise<void> {
    try {
      const mainElement = document.querySelector('main') || document.body;
      const styledElement = this.preserveElementState(mainElement as HTMLElement);
      await this.convertImagesToBase64(styledElement);

      const cssRules = this.extractAllCSSRules();
      const cssContent = this.generateComprehensiveCSS(cssRules);
      const jsContent = this.generateEnhancedJS();

      const pageTitle = options.pageTitle || document.title || 'Ticket Tracker Export';
      const bodyHTML = styledElement.outerHTML;

      const fullHTML = this.generateEnhancedHTMLDocument(bodyHTML, pageTitle);

      let filename = options.filename || 'ticket_tracker_export';
      if (options.includeTimestamp !== false) {
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
        filename = `${filename}_${timestamp}`;
      }
      filename = this.sanitizeFilename(filename);

      const zip = new JSZip();
      zip.file('index.html', fullHTML);
      zip.file('styles.css', cssContent);
      zip.file('script.js', jsContent);
      zip.file('README.md', this.generateReadme(options));

      const zipBlob = await zip.generateAsync({ type: 'blob' });
      const url = URL.createObjectURL(zipBlob);

      const link = document.createElement('a');
      link.href = url;
      link.download = `${filename}.zip`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      setTimeout(() => URL.revokeObjectURL(url), 100);
    } catch (error) {
      console.error('Error exporting HTML:', error);
      throw new Error('Failed to export HTML. Please try again.');
    }
  }

  async exportFullPage(options: ExportOptions = {}): Promise<void> {
    try {
      const rootElement = document.getElementById('root') || document.body;
      const styledElement = this.preserveElementState(rootElement as HTMLElement);
      await this.convertImagesToBase64(styledElement);

      const cssRules = this.extractAllCSSRules();
      const cssContent = this.generateComprehensiveCSS(cssRules);
      const jsContent = this.generateEnhancedJS();

      const pageTitle = options.pageTitle || document.title || 'Ticket Tracker Export';
      const bodyHTML = styledElement.outerHTML;

      const fullHTML = this.generateEnhancedHTMLDocument(bodyHTML, pageTitle);

      let filename = options.filename || 'ticket_tracker_full_export';
      if (options.includeTimestamp !== false) {
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
        filename = `${filename}_${timestamp}`;
      }
      filename = this.sanitizeFilename(filename);

      const zip = new JSZip();
      zip.file('index.html', fullHTML);
      zip.file('styles.css', cssContent);
      zip.file('script.js', jsContent);
      zip.file('README.md', this.generateReadme(options));

      const zipBlob = await zip.generateAsync({ type: 'blob' });
      const url = URL.createObjectURL(zipBlob);

      const link = document.createElement('a');
      link.href = url;
      link.download = `${filename}.zip`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      setTimeout(() => URL.revokeObjectURL(url), 100);
    } catch (error) {
      console.error('Error exporting full page HTML:', error);
      throw new Error('Failed to export HTML. Please try again.');
    }
  }
}

export const htmlExportService = new HTMLExportService();
