import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';

interface ChatPrefillState {
  message: string | null;
  setPrefill: (msg: string) => void;
  consumePrefill: () => string | null;
}

const ChatPrefillContext = createContext<ChatPrefillState>({
  message: null,
  setPrefill: () => {},
  consumePrefill: () => null,
});

export function ChatPrefillProvider({ children }: { children: ReactNode }) {
  const [message, setMessage] = useState<string | null>(null);

  const setPrefill = useCallback((msg: string) => {
    setMessage(msg);
  }, []);

  const consumePrefill = useCallback(() => {
    const current = message;
    setMessage(null);
    return current;
  }, [message]);

  return (
    <ChatPrefillContext.Provider value={{ message, setPrefill, consumePrefill }}>
      {children}
    </ChatPrefillContext.Provider>
  );
}

export function useChatPrefill() {
  return useContext(ChatPrefillContext);
}
