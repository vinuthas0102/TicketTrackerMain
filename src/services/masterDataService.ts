import { supabase, isSupabaseAvailable } from '../lib/supabase';

export interface MasterItem {
  id: string;
  name: string;
  is_active: boolean;
  display_order: number;
  created_at: string;
  updated_at: string;
}

export interface MasterConfig {
  key: string;
  value: string;
  description: string;
}

export type MasterDataType = 'categories' | 'departments' | 'locations' | 'properties';

const TABLE_MAP: Record<MasterDataType, string> = {
  categories: 'master_categories',
  departments: 'master_departments',
  locations: 'master_locations',
  properties: 'master_properties',
};

const FALLBACK_CATEGORIES = [
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
];

const FALLBACK_LOCATIONS = ['Location01', 'Location02'];
const FALLBACK_PROPERTIES = ['PROP001', 'PROP002'];

const FALLBACK_DEPARTMENTS: string[] = [];

export class MasterDataService {
  static async getAll(type: MasterDataType, moduleId?: string): Promise<MasterItem[]> {
    if (!isSupabaseAvailable()) {
      return this.getFallback(type);
    }
    try {
      const table = TABLE_MAP[type];
      let query = supabase.from(table).select('*');
      if (moduleId && type === 'categories') {
        query = query.eq('module_id', moduleId);
      }
      const { data, error } = await query.order('display_order', { ascending: true });

      if (error) throw error;
      return (data || []) as MasterItem[];
    } catch (error) {
      console.error(`Error fetching master ${type}:`, error);
      return this.getFallback(type);
    }
  }

  static async getActive(type: MasterDataType, moduleId?: string): Promise<string[]> {
    const items = await this.getAll(type, moduleId);
    return items.filter(i => i.is_active).map(i => i.name);
  }

  static async add(type: MasterDataType, name: string, moduleId?: string): Promise<MasterItem | null> {
    if (!isSupabaseAvailable()) return null;
    try {
      const table = TABLE_MAP[type];
      let existingQuery = supabase.from(table).select('id').ilike('name', name);
      if (moduleId && type === 'categories') {
        existingQuery = existingQuery.eq('module_id', moduleId);
      }
      const { data: existing } = await existingQuery.maybeSingle();

      if (existing) throw new Error(`"${name}" already exists`);

      let maxOrderQuery = supabase.from(table).select('display_order');
      if (moduleId && type === 'categories') {
        maxOrderQuery = maxOrderQuery.eq('module_id', moduleId);
      }
      const { data: maxOrder } = await maxOrderQuery.order('display_order', { ascending: false }).limit(1).maybeSingle();

      const nextOrder = (maxOrder?.display_order || 0) + 1;

      const insertRow: Record<string, unknown> = { name, is_active: true, display_order: nextOrder };
      if (moduleId && type === 'categories') {
        insertRow.module_id = moduleId;
      }
      const { data, error } = await supabase
        .from(table)
        .insert([insertRow])
        .select()
        .single();

      if (error) throw error;
      return data as MasterItem;
    } catch (error) {
      console.error(`Error adding master ${type}:`, error);
      throw error;
    }
  }

  static async remove(type: MasterDataType, id: string): Promise<void> {
    if (!isSupabaseAvailable()) return;
    try {
      const table = TABLE_MAP[type];
      const { error } = await supabase.from(table).delete().eq('id', id);
      if (error) throw error;
    } catch (error) {
      console.error(`Error removing master ${type}:`, error);
      throw error;
    }
  }

  static async toggleActive(type: MasterDataType, id: string, isActive: boolean): Promise<void> {
    if (!isSupabaseAvailable()) return;
    try {
      const table = TABLE_MAP[type];
      const { error } = await supabase.from(table).update({ is_active: isActive }).eq('id', id);
      if (error) throw error;
    } catch (error) {
      console.error(`Error toggling master ${type}:`, error);
      throw error;
    }
  }

  static async getConfig(key: string): Promise<string | null> {
    if (!isSupabaseAvailable()) return null;
    try {
      const { data, error } = await supabase
        .from('master_config')
        .select('value')
        .eq('key', key)
        .maybeSingle();

      if (error) throw error;
      return data?.value || null;
    } catch (error) {
      console.error(`Error fetching config ${key}:`, error);
      return null;
    }
  }

  static async setConfig(key: string, value: string, description?: string): Promise<void> {
    if (!isSupabaseAvailable()) return;
    try {
      const { error } = await supabase
        .from('master_config')
        .upsert({ key, value, description }, { onConflict: 'key' });
      if (error) throw error;
    } catch (error) {
      console.error(`Error setting config ${key}:`, error);
      throw error;
    }
  }

  static async getModuleCode(moduleId: string): Promise<string> {
    if (!isSupabaseAvailable()) return 'TKT';
    try {
      const { data, error } = await supabase
        .from('modules')
        .select('config')
        .eq('id', moduleId)
        .maybeSingle();

      if (error) throw error;
      return data?.config?.moduleCode || 'TKT';
    } catch (error) {
      console.error('Error fetching module code:', error);
      return 'TKT';
    }
  }

  static async setModuleCode(moduleId: string, moduleCode: string): Promise<void> {
    if (!isSupabaseAvailable()) return;
    try {
      const { data: module, error: fetchError } = await supabase
        .from('modules')
        .select('config')
        .eq('id', moduleId)
        .maybeSingle();

      if (fetchError) throw fetchError;
      if (!module) throw new Error('Module not found');

      const updatedConfig = { ...(module.config || {}), moduleCode };
      const { error } = await supabase
        .from('modules')
        .update({ config: updatedConfig })
        .eq('id', moduleId);

      if (error) throw error;
    } catch (error) {
      console.error('Error setting module code:', error);
      throw error;
    }
  }

  static async generateTicketNumber(locationPrefix: string, moduleCode: string): Promise<string> {
    if (!isSupabaseAvailable()) {
      return `TKT-${Date.now()}`;
    }
    try {
      const companyCode = await this.getConfig('company_code') || 'NMDC';

      let effectivePrefix = locationPrefix;
      if (!effectivePrefix) {
        const activeLocations = await this.getActive('locations');
        effectivePrefix = activeLocations.length > 0 ? activeLocations[0] : 'LOC';
      }
      const loc3 = effectivePrefix.substring(0, 3).toUpperCase();

      const { data: existing, error: selectError } = await supabase
        .from('ticket_number_counter')
        .select('id, counter')
        .eq('location_prefix', loc3)
        .eq('module_code', moduleCode)
        .maybeSingle();

      if (selectError) throw selectError;

      let nextCounter: number;

      if (existing) {
        nextCounter = existing.counter + 1;
        const { error: updateError } = await supabase
          .from('ticket_number_counter')
          .update({ counter: nextCounter })
          .eq('id', existing.id);

        if (updateError) throw updateError;
      } else {
        nextCounter = 1;
        const { error: insertError } = await supabase
          .from('ticket_number_counter')
          .insert([{ location_prefix: loc3, module_code: moduleCode, counter: nextCounter }]);

        if (insertError) throw insertError;
      }

      const paddedCounter = String(nextCounter).padStart(6, '0');
      return `TKT-${companyCode}${loc3}-${moduleCode}-${paddedCounter}`;
    } catch (error) {
      console.error('Error generating ticket number:', error);
      return `TKT-${Date.now()}`;
    }
  }

  private static getFallback(type: MasterDataType): MasterItem[] {
    let names: string[] = [];
    if (type === 'categories') names = FALLBACK_CATEGORIES;
    else if (type === 'locations') names = FALLBACK_LOCATIONS;
    else if (type === 'properties') names = FALLBACK_PROPERTIES;

    return names.map((name, i) => ({
      id: `fallback-${type}-${i}`,
      name,
      is_active: true,
      display_order: i + 1,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    }));
  }
}
