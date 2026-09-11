import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';

type Confirm = (message: string, destructive?: boolean) => Promise<boolean>;
const Context = createContext<Confirm | null>(null);
export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [request, setRequest] = useState<{ message: string; destructive: boolean } | null>(null);
  const resolver = useRef<((answer: boolean) => void) | null>(null);
  const confirm = useCallback<Confirm>((message, destructive = true) => {
    resolver.current?.(false);
    return new Promise<boolean>(resolve => { resolver.current = resolve; setRequest({ message, destructive }); });
  }, []);
  const answer = (value: boolean) => { resolver.current?.(value); resolver.current = null; setRequest(null); };
  useEffect(() => () => { resolver.current?.(false); }, []);
  return <Context.Provider value={confirm}>{children}<ConfirmDialog isOpen={request !== null}
    title={request?.destructive ? 'Xác nhận xóa' : 'Bỏ thay đổi chưa lưu?'} message={request?.message || ''}
    isDestructive={request?.destructive} confirmText={request?.destructive ? 'Xóa' : 'Bỏ thay đổi'}
    onClose={() => answer(false)} onConfirm={() => answer(true)} /></Context.Provider>;
}
export function useConfirm() {
  const confirm = useContext(Context);
  if (!confirm) throw new Error('Thiếu bộ cung cấp hộp thoại xác nhận');
  return confirm;
}
