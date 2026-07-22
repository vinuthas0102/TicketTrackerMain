import { apiClient } from '../lib/apiClient';
import { API_ENDPOINTS } from '../lib/apiEndpoints';

export class DependencyService {
  static async createDependencies(
    stepId: string,
    dependentOnStepIds: string[],
    createdBy: string
  ): Promise<void> {
    try {
      await apiClient.post(API_ENDPOINTS.DEPENDENCIES.CREATE, {
        stepId,
        dependentOnStepIds,
        createdBy,
      });
    } catch (error) {
      console.error('Error creating dependencies:', error);
      throw error;
    }
  }

  static async getDependencies(stepId: string): Promise<any[]> {
    try {
      const dependencies = await apiClient.get<any[]>(API_ENDPOINTS.DEPENDENCIES.LIST, {
        stepId,
      });
      return dependencies;
    } catch (error) {
      console.error('Error fetching dependencies:', error);
      return [];
    }
  }

  static async getDependentSteps(stepId: string): Promise<any[]> {
    try {
      return await apiClient.get<any[]>(`/workflow-steps/${stepId}/dependent-steps`);
    } catch (error) {
      console.error('Error fetching dependent steps:', error);
      return [];
    }
  }

  static async getStepsWithDependencies(ticketId: string): Promise<any[]> {
    try {
      const steps = await apiClient.get<any[]>(`/tickets/${ticketId}/workflow-steps`);
      for (const step of steps) {
        if (step.id) {
          step.dependencies = await this.getDependencies(step.id);
        }
      }
      return steps;
    } catch (error) {
      console.error('Error fetching steps with dependencies:', error);
      return [];
    }
  }

  static async deleteDependency(dependencyId: string, deletedBy: string): Promise<void> {
    try {
      await apiClient.delete(
        API_ENDPOINTS.DEPENDENCIES.DELETE(dependencyId) + `?deletedBy=${deletedBy}`
      );
    } catch (error) {
      console.error('Error deleting dependency:', error);
      throw error;
    }
  }

  static async lockStepDependencies(stepId: string): Promise<void> {
    try {
      await apiClient.put(`/steps/${stepId}/lock-dependencies`, {});
    } catch (error) {
      console.error('Error locking dependencies:', error);
      throw error;
    }
  }

  static async unlockStepDependencies(stepId: string, unlockedBy: string): Promise<void> {
    try {
      await apiClient.put(`/steps/${stepId}/unlock-dependencies`, { unlockedBy });
    } catch (error) {
      console.error('Error unlocking dependencies:', error);
      throw error;
    }
  }

  static async checkDependenciesCompleted(stepId: string): Promise<boolean> {
    try {
      const response = await apiClient.get<{ allCompleted: boolean }>(
        `/steps/${stepId}/dependencies/status`
      );
      return response.allCompleted;
    } catch (error) {
      console.error('Error checking dependencies:', error);
      return false;
    }
  }

  static async validateStepCompletion(
    step: any,
    allSteps: any[]
  ): Promise<any> {
    try {
      if (step.is_parallel !== false) {
        return {
          canComplete: true,
          incompleteDependencies: [],
          dependencyMode: step.dependency_mode || 'all',
        };
      }

      const dependencies = await this.getDependencies(step.id);

      if (dependencies.length === 0) {
        return {
          canComplete: true,
          incompleteDependencies: [],
          dependencyMode: step.dependency_mode || 'all',
        };
      }

      const dependencyStepIds = dependencies.map((d: any) => d.dependsOnStepId || d.depends_on_step_id);
      const dependentSteps = allSteps.filter((s: any) => dependencyStepIds.includes(s.id));

      const completedDependencies = dependentSteps.filter(
        (s: any) => s.status === 'COMPLETED' || s.status === 'CLOSED'
      );

      const incompleteDependencies = dependentSteps.filter(
        (s: any) => s.status !== 'COMPLETED' && s.status !== 'CLOSED'
      );

      const dependencyMode = step.dependency_mode || 'all';

      let canComplete = false;
      let message = '';

      if (dependencyMode === 'all') {
        canComplete = incompleteDependencies.length === 0;
        if (!canComplete) {
          message = `All ${dependentSteps.length} dependencies must be completed. ${incompleteDependencies.length} remaining.`;
        }
      } else {
        canComplete = completedDependencies.length > 0;
        if (!canComplete) {
          message = `At least one of ${dependentSteps.length} dependencies must be completed.`;
        }
      }

      return {
        canComplete,
        incompleteDependencies,
        message,
        dependencyMode,
      };
    } catch (error) {
      console.error('Error validating step completion:', error);
      return {
        canComplete: false,
        incompleteDependencies: [],
        message: 'Error validating dependencies',
        dependencyMode: step.dependency_mode || 'all',
      };
    }
  }

  static getAvailableDependencySteps(
    currentStep: any | null,
    allSteps: any[]
  ): any[] {
    if (!currentStep) {
      return allSteps.filter((s: any) =>
        (s.level_2 === 0 && s.level_3 === 0)
      );
    }

    return allSteps.filter((step: any) => {
      if (step.id === currentStep.id) return false;

      if (step.parentStepId === currentStep.id) return false;

      if (currentStep.parentStepId && step.id === currentStep.parentStepId) return false;

      const isDescendant = this.isDescendantOf(step, currentStep, allSteps);
      if (isDescendant) return false;

      if (currentStep.level_1 && step.level_1) {
        if (currentStep.level_2 === 0 && currentStep.level_3 === 0) {
          return step.level_2 === 0 && step.level_3 === 0 && step.level_1 < currentStep.level_1;
        } else if (currentStep.level_2 > 0 && currentStep.level_3 === 0) {
          return (
            (step.level_2 === 0 && step.level_3 === 0) ||
            (step.level_1 === currentStep.level_1 && step.level_2 > 0 && step.level_3 === 0 && step.level_2 < currentStep.level_2)
          );
        } else if (currentStep.level_3 > 0) {
          return (
            (step.level_2 === 0 && step.level_3 === 0) ||
            (step.level_1 === currentStep.level_1 && step.level_2 > 0 && step.level_3 === 0) ||
            (step.level_1 === currentStep.level_1 &&
             step.level_2 === currentStep.level_2 &&
             step.level_3 > 0 &&
             step.level_3 < currentStep.level_3)
          );
        }
      }

      return true;
    });
  }

  private static isDescendantOf(
    step: any,
    potentialAncestor: any,
    allSteps: any[]
  ): boolean {
    let current: any | undefined = step;

    while (current && current.parentStepId) {
      if (current.parentStepId === potentialAncestor.id) {
        return true;
      }
      current = allSteps.find((s: any) => s.id === current!.parentStepId);
    }

    return false;
  }

  static formatDependencyStatus(
    step: any,
    dependentSteps: any[]
  ): string {
    if (step.is_parallel !== false || !dependentSteps || dependentSteps.length === 0) {
      return '';
    }

    const completed = dependentSteps.filter(
      (s: any) => s.status === 'COMPLETED' || s.status === 'CLOSED'
    ).length;

    const mode = step.dependency_mode || 'all';

    if (mode === 'all') {
      return `${completed}/${dependentSteps.length} dependencies completed`;
    } else {
      return completed > 0
        ? `${completed}/${dependentSteps.length} dependencies completed (any one required)`
        : `0/${dependentSteps.length} dependencies completed (any one required)`;
    }
  }
}
