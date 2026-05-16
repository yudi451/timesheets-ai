import { useState } from 'react';
import { sendDiscrepancyEmail, type SendEmailResult } from '../api/dashboard';

interface Props {
  defaultRecipient: string;
}

type Status =
  | { kind: 'idle' }
  | { kind: 'confirming' }
  | { kind: 'sending' }
  | { kind: 'sent'; result: SendEmailResult }
  | { kind: 'error'; message: string };

export function SendEmailButton({ defaultRecipient }: Props) {
  const [status, setStatus] = useState<Status>({ kind: 'idle' });

  async function doSend() {
    setStatus({ kind: 'sending' });
    try {
      const result = await sendDiscrepancyEmail();
      if (!result.sent) {
        setStatus({ kind: 'error', message: result.statusMessage || 'Send failed' });
        return;
      }
      setStatus({ kind: 'sent', result });
    } catch (e) {
      setStatus({ kind: 'error', message: String(e) });
    }
  }

  if (status.kind === 'sending') {
    return (
      <button
        disabled
        className="rounded-md bg-info-500 px-4 py-2 text-sm font-medium text-white opacity-70"
      >
        Sending email…
      </button>
    );
  }

  if (status.kind === 'sent') {
    return (
      <div className="flex items-center gap-2 rounded-md bg-ok-50 px-4 py-2 text-sm text-ok-700 ring-1 ring-ok-500/30">
        <span>✓ Sent to {status.result.recipient}</span>
        <button
          onClick={() => setStatus({ kind: 'idle' })}
          className="text-xs underline opacity-70 hover:opacity-100"
        >
          send again
        </button>
      </div>
    );
  }

  if (status.kind === 'error') {
    return (
      <div className="flex items-center gap-2 rounded-md bg-risk-50 px-4 py-2 text-sm text-risk-700 ring-1 ring-risk-500/30">
        <span title={status.message}>✗ Send failed</span>
        <button
          onClick={() => setStatus({ kind: 'idle' })}
          className="text-xs underline opacity-70 hover:opacity-100"
        >
          try again
        </button>
      </div>
    );
  }

  if (status.kind === 'confirming') {
    return (
      <div className="flex items-center gap-2 rounded-md bg-warn-50 px-4 py-2 text-sm text-warn-700 ring-1 ring-warn-500/30">
        <span>Send summary to {defaultRecipient}?</span>
        <button
          onClick={doSend}
          className="rounded bg-warn-500 px-2 py-0.5 text-xs font-medium text-white hover:bg-warn-700"
        >
          Confirm
        </button>
        <button
          onClick={() => setStatus({ kind: 'idle' })}
          className="text-xs underline opacity-70 hover:opacity-100"
        >
          Cancel
        </button>
      </div>
    );
  }

  return (
    <button
      onClick={() => setStatus({ kind: 'confirming' })}
      className="rounded-md bg-info-500 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-info-700"
    >
      Email summary to manager
    </button>
  );
}
