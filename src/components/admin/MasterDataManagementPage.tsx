import React, { useState, useEffect, useCallback } from 'react';
import { Database, Plus, Trash2, Settings, Tag, Building2, MapPin, AlertCircle, CheckCircle } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { MasterDataService, MasterItem, MasterDataType } from '../../services/masterDataService';
import { supabase } from '../../lib/supabase';
import LoadingSpinner from '../common/LoadingSpinner';

type TabType = 'categories' | 'departments' | 'locations' | 'config';

interface ModuleInfo {
  id: string;
  name: string;
  moduleCode: string;
}

const MasterDataManagementPage: React.FC = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<TabType>('categories');
  const [items, setItems] = useState<MasterItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [newItemName, setNewItemName] = useState('');
  const [adding, setAdding] = useState(false);
  const [companyCode, setCompanyCode] = useState('');
  const [companyCodeOriginal, setCompanyCodeOriginal] = useState('');
  const [savingConfig, setSavingConfig] = useState(false);
  const [configSaved, setConfigSaved] = useState(false);
  const [modules, setModules] = useState<ModuleInfo[]>([]);
  const [moduleCodeUpdates, setModuleCodeUpdates] = useState<Record<string, string>>({});

  const loadItems = useCallback(async (type: MasterDataType) => {
    setLoading(true);
    setError(null);
    try {
      const data = await MasterDataService.getAll(type);
      setItems(data);
    } catch (err) {
      setError('Failed to load data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadConfig = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const code = await MasterDataService.getConfig('company_code') || 'NMDC';
      setCompanyCode(code);
      setCompanyCodeOriginal(code);

      const { data: moduleData } = await supabase
        .from('modules')
        .select('id, name, config')
        .eq('active', true)
        .order('name');

      const moduleList: ModuleInfo[] = (moduleData || []).map((m: any) => ({
        id: m.id,
        name: m.name,
        moduleCode: m.config?.moduleCode || 'TKT',
      }));
      setModules(moduleList);

      const updates: Record<string, string> = {};
      moduleList.forEach(m => { updates[m.id] = m.moduleCode; });
      setModuleCodeUpdates(updates);
    } catch (err) {
      setError('Failed to load configuration');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (activeTab === 'config') {
      loadConfig();
    } else {
      loadItems(activeTab as MasterDataType);
    }
  }, [activeTab, loadItems, loadConfig]);

  const handleAdd = async () => {
    const trimmed = newItemName.trim();
    if (!trimmed) return;
    setAdding(true);
    setError(null);
    try {
      await MasterDataService.add(activeTab as MasterDataType, trimmed);
      setNewItemName('');
      await loadItems(activeTab as MasterDataType);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add item');
    } finally {
      setAdding(false);
    }
  };

  const handleDelete = async (id: string, name: string) => {
    if (!confirm(`Are you sure you want to delete "${name}"?`)) return;
    try {
      await MasterDataService.remove(activeTab as MasterDataType, id);
      await loadItems(activeTab as MasterDataType);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete item');
    }
  };

  const handleToggleActive = async (item: MasterItem) => {
    try {
      await MasterDataService.toggleActive(activeTab as MasterDataType, item.id, !item.is_active);
      await loadItems(activeTab as MasterDataType);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update item');
    }
  };

  const handleSaveConfig = async () => {
    setSavingConfig(true);
    setError(null);
    setConfigSaved(false);
    try {
      await MasterDataService.setConfig('company_code', companyCode.trim() || 'NMDC', 'Company code used in ticket number generation');
      for (const module of modules) {
        const newCode = moduleCodeUpdates[module.id]?.trim() || 'TKT';
        if (newCode !== module.moduleCode) {
          await MasterDataService.setModuleCode(module.id, newCode);
        }
      }
      setCompanyCodeOriginal(companyCode.trim() || 'NMDC');
      setConfigSaved(true);
      setTimeout(() => setConfigSaved(false), 3000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save configuration');
    } finally {
      setSavingConfig(false);
    }
  };

  if (user?.role !== 'EO') {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center">
          <div className="text-6xl mb-4">🔒</div>
          <h2 className="text-2xl font-bold text-gray-800 mb-2">Access Denied</h2>
          <p className="text-gray-600">Only EO users can access Master Data Management.</p>
        </div>
      </div>
    );
  }

  const tabs: { key: TabType; label: string; icon: React.ReactNode }[] = [
    { key: 'categories', label: 'Categories', icon: <Tag className="w-4 h-4" /> },
    { key: 'departments', label: 'Departments', icon: <Building2 className="w-4 h-4" /> },
    { key: 'locations', label: 'Locations', icon: <MapPin className="w-4 h-4" /> },
    { key: 'config', label: 'System Config', icon: <Settings className="w-4 h-4" /> },
  ];

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-3">
            <Database className="w-6 h-6 text-blue-600" />
            <div>
              <h2 className="text-2xl font-bold text-gray-900">Master Data Setup</h2>
              <p className="text-sm text-gray-600">Manage categories, departments, locations, and system configuration</p>
            </div>
          </div>
        </div>

        {/* Tab Bar */}
        <div className="flex border-b border-gray-200 mb-6">
          {tabs.map(tab => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`flex items-center space-x-2 px-5 py-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.key
                  ? 'border-blue-600 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.icon}
              <span>{tab.label}</span>
            </button>
          ))}
        </div>

        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 rounded-md p-4 text-red-700 text-sm flex items-center gap-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            {error}
          </div>
        )}

        {loading && (
          <div className="flex justify-center py-8">
            <LoadingSpinner />
          </div>
        )}

        {/* Master Data Tabs (categories, departments, locations) */}
        {activeTab !== 'config' && !loading && (
          <>
            <div className="flex gap-3 mb-6">
              <input
                type="text"
                value={newItemName}
                onChange={(e) => setNewItemName(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleAdd()}
                placeholder={`Add new ${activeTab === 'categories' ? 'category' : activeTab === 'departments' ? 'department' : 'location'}...`}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
              />
              <button
                onClick={handleAdd}
                disabled={adding || !newItemName.trim()}
                className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed text-sm"
              >
                <Plus className="w-4 h-4" />
                Add
              </button>
            </div>

            {items.length === 0 ? (
              <div className="bg-gray-50 border border-gray-200 rounded-md p-8 text-center text-gray-500">
                <p>No {activeTab} found. Add one above to get started.</p>
              </div>
            ) : (
              <div className="space-y-2">
                {items.map((item) => (
                  <div
                    key={item.id}
                    className={`flex items-center justify-between border rounded-lg p-3 transition-shadow hover:shadow-sm ${
                      item.is_active ? 'border-gray-200 bg-white' : 'border-gray-300 bg-gray-100 opacity-75'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <span className="text-xs text-gray-400 font-mono w-8">#{item.display_order}</span>
                      <span className="text-sm font-medium text-gray-900">{item.name}</span>
                      <span className={`text-xs px-2 py-0.5 rounded ${item.is_active ? 'bg-green-100 text-green-800' : 'bg-gray-200 text-gray-600'}`}>
                        {item.is_active ? 'Active' : 'Inactive'}
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleToggleActive(item)}
                        className={`p-1.5 rounded transition-colors ${item.is_active ? 'text-orange-600 hover:bg-orange-50' : 'text-green-600 hover:bg-green-50'}`}
                        title={item.is_active ? 'Deactivate' : 'Activate'}
                      >
                        {item.is_active ? <AlertCircle className="w-4 h-4" /> : <CheckCircle className="w-4 h-4" />}
                      </button>
                      <button
                        onClick={() => handleDelete(item.id, item.name)}
                        className="p-1.5 text-red-600 hover:bg-red-50 rounded transition-colors"
                        title="Delete"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </>
        )}

        {/* Config Tab */}
        {activeTab === 'config' && !loading && (
          <div className="space-y-8">
            <div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Company Code</h3>
              <p className="text-sm text-gray-600 mb-4">
                This code appears in every ticket number. Example: <code className="bg-gray-100 px-2 py-0.5 rounded text-xs">TKT-{companyCode || 'NMDC'}LOC-MAINT-000001</code>
              </p>
              <div className="flex gap-3 items-center">
                <input
                  type="text"
                  value={companyCode}
                  onChange={(e) => setCompanyCode(e.target.value.toUpperCase())}
                  placeholder="NMDC"
                  maxLength={10}
                  className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm w-48"
                />
                {configSaved && (
                  <span className="text-sm text-green-600 flex items-center gap-1">
                    <CheckCircle className="w-4 h-4" /> Saved!
                  </span>
                )}
              </div>
            </div>

            <div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Module Codes</h3>
              <p className="text-sm text-gray-600 mb-4">
                Each module has a code that appears in ticket numbers. Example: <code className="bg-gray-100 px-2 py-0.5 rounded text-xs">TKT-NMDCLOC-MAINT-000001</code>
              </p>
              {modules.length === 0 ? (
                <p className="text-sm text-gray-500">No modules found.</p>
              ) : (
                <div className="space-y-2">
                  {modules.map((m) => (
                    <div key={m.id} className="flex items-center justify-between border border-gray-200 rounded-lg p-3 bg-white">
                      <span className="text-sm font-medium text-gray-900">{m.name}</span>
                      <div className="flex items-center gap-2">
                        <span className="text-xs text-gray-400">Current: {m.moduleCode}</span>
                        <input
                          type="text"
                          value={moduleCodeUpdates[m.id] || ''}
                          onChange={(e) => setModuleCodeUpdates(prev => ({ ...prev, [m.id]: e.target.value.toUpperCase() }))}
                          placeholder={m.moduleCode}
                          maxLength={10}
                          className="px-3 py-1.5 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm w-32 text-center font-mono"
                        />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="pt-4 border-t border-gray-200">
              <button
                onClick={handleSaveConfig}
                disabled={savingConfig}
                className="flex items-center gap-2 px-6 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed text-sm font-medium"
              >
                <Settings className="w-4 h-4" />
                {savingConfig ? 'Saving...' : 'Save Configuration'}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default MasterDataManagementPage;
