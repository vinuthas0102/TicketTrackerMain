import React, { useState, useRef, useEffect } from 'react';
import { User as UserType } from '../../types';

interface SapIdLinkProps {
  user?: UserType;
  fallback?: string;
  className?: string;
}

const roleLabels: Record<string, string> = {
  EMPLOYEE: 'Employee',
  DO: 'Department Officer',
  EO: 'Executive Officer',
  VENDOR: 'Vendor',
  FINANCE: 'Finance',
  TECHNICIAN: 'Technician',
};

const SapIdLink: React.FC<SapIdLinkProps> = ({ user, fallback = '—', className = '' }) => {
  const [showPopover, setShowPopover] = useState(false);
  const containerRef = useRef<HTMLSpanElement>(null);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleEnter = () => {
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    setShowPopover(true);
  };

  const handleLeave = () => {
    timeoutRef.current = setTimeout(() => setShowPopover(false), 200);
  };

  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, []);

  if (!user || !user.sapId) {
    return <span className={className}>{fallback}</span>;
  }

  return (
    <span
      ref={containerRef}
      className="relative inline-block"
      onMouseEnter={handleEnter}
      onMouseLeave={handleLeave}
      onClick={(e) => { e.stopPropagation(); setShowPopover(s => !s); }}
    >
      <a
        href="#"
        onClick={(e) => e.preventDefault()}
        className={`text-blue-600 hover:text-blue-800 hover:underline cursor-pointer font-semibold ${className}`}
        title="Click to view user details"
      >
        {user.sapId}
      </a>
      {showPopover && (
        <div
          className="absolute z-50 left-0 top-full mt-1 w-64 bg-white rounded-lg shadow-xl border border-gray-200 p-4"
          onMouseEnter={handleEnter}
          onMouseLeave={handleLeave}
        >
          <div className="flex items-center gap-3 mb-3">
            <div className="h-10 w-10 rounded-full bg-gradient-to-br from-blue-500 to-teal-600 flex items-center justify-center text-white font-semibold text-sm shrink-0">
              {user.name.charAt(0).toUpperCase()}
            </div>
            <div className="min-w-0">
              <p className="text-sm font-bold text-gray-900 truncate">{user.name}</p>
              <p className="text-xs text-gray-500 truncate">{roleLabels[user.role] || user.role}</p>
            </div>
          </div>
          <dl className="space-y-1.5 text-xs">
            <div className="flex gap-2">
              <dt className="text-gray-400 font-medium shrink-0 w-16">Email</dt>
              <dd className="text-gray-700 break-all">{user.email}</dd>
            </div>
            <div className="flex gap-2">
              <dt className="text-gray-400 font-medium shrink-0 w-16">Dept</dt>
              <dd className="text-gray-700">{user.department}</dd>
            </div>
            <div className="flex gap-2">
              <dt className="text-gray-400 font-medium shrink-0 w-16">SAP ID</dt>
              <dd className="text-gray-700 font-mono">{user.sapId}</dd>
            </div>
            {user.regions && user.regions.length > 0 && (
              <div className="flex gap-2">
                <dt className="text-gray-400 font-medium shrink-0 w-16">Regions</dt>
                <dd className="text-gray-700">{user.regions.join(', ')}</dd>
              </div>
            )}
          </dl>
        </div>
      )}
    </span>
  );
};

export default SapIdLink;
