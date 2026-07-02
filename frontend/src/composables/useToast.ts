import { ref } from "vue";

export type ToastVariant = "success" | "error" | "info" | "warning";

export interface ToastMessage {
  id: number;
  message: string;
  variant: ToastVariant;
}

const toast = ref<ToastMessage | null>(null);
let toastId = 0;
let toastTimer: number | null = null;

export function useToast() {
  function showToast(message: string, variant: ToastVariant = "success") {
    toastId += 1;
    toast.value = { id: toastId, message, variant };

    if (toastTimer !== null) {
      window.clearTimeout(toastTimer);
    }

    toastTimer = window.setTimeout(() => {
      toast.value = null;
      toastTimer = null;
    }, 2200);
  }

  function clearToast() {
    toast.value = null;
    if (toastTimer !== null) {
      window.clearTimeout(toastTimer);
      toastTimer = null;
    }
  }

  return {
    toast,
    showToast,
    clearToast,
  };
}
