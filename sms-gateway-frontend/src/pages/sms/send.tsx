import {
  Button,
  Card,
  Form,
  Grid,
  Input,
  Typography,
  Alert,
  Table,
  Tag,
  Tooltip,
} from "antd";
import type { ColumnsType, TablePaginationConfig } from "antd/es/table";
import { SendOutlined, ReloadOutlined, SwapOutlined } from "@ant-design/icons";
import { useState, useEffect, useCallback, useRef, useMemo } from "react";
import { useNavigate } from "react-router";
import { API_URL } from "../../providers/constants";

const { TextArea } = Input;
const { Title } = Typography;
const isMock = import.meta.env.VITE_MOCK === "true";

type MessageStatus = "PENDING" | "SENT" | "DELIVERED" | "FAILED" | "UNKNOWN";

interface MessageRecord {
  id: number;
  phoneNumber: string;
  content: string;
  status: MessageStatus;
  createdAt: string;
}

interface PageResponse {
  content: MessageRecord[];
  totalElements: number;
  number: number;
  size: number;
}

interface SmsStatusSocketEvent {
  type: "message_status";
  message: MessageRecord;
}

interface SmsSocketControlEvent {
  type: "subscribed" | "unsubscribed" | "error";
  messageId?: number;
  message?: string;
}

async function getAuthHeaders(): Promise<Record<string, string>> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (!isMock) {
    const { userManager } = await import("../../providers/auth-provider");
    const user = await userManager.getUser();
    if (user?.access_token) {
      headers["Authorization"] = `Bearer ${user.access_token}`;
    }
  }
  return headers;
}

async function getAccessToken(): Promise<string | null> {
  if (isMock) {
    return null;
  }

  const { userManager } = await import("../../providers/auth-provider");
  const user = await userManager.getUser();
  return user?.access_token ?? null;
}

function buildSmsStatusWebSocketUrl(accessToken: string): string {
  const url = new URL("/ws/sms", API_URL);
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
  url.searchParams.set("access_token", accessToken);
  return url.toString();
}

function upsertMessage(messages: MessageRecord[], message: MessageRecord, maxItems?: number): MessageRecord[] {
  const next = [message, ...messages.filter((entry) => entry.id !== message.id)].sort(
    (left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime(),
  );

  return typeof maxItems === "number" ? next.slice(0, maxItems) : next;
}

const STATUS_CONFIG: Record<string, { color: string; label: string }> = {
  SENT: { color: "warning", label: "Sent" },
  PENDING: { color: "processing", label: "Pending" },
  DELIVERED: { color: "success", label: "Delivered" },
  FAILED: { color: "error", label: "Failed" },
  UNKNOWN: { color: "default", label: "Unknown" },
};

export const SmsSend: React.FC = () => {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState<{ type: "success" | "error"; message: string } | null>(null);

  const [messages, setMessages] = useState<MessageRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState<TablePaginationConfig>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showTotal: (total) => `${total} messages`,
  });
  const socketRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<number | null>(null);
  const subscribedIdsRef = useRef<Set<number>>(new Set());
  const paginationRef = useRef<TablePaginationConfig>(pagination);

  useEffect(() => {
    paginationRef.current = pagination;
  }, [pagination]);

  const fetchMessages = useCallback(async (page = 1, size = 10) => {
    setLoading(true);
    try {
      const headers = await getAuthHeaders();
      const response = await fetch(
        `${API_URL}/api/v1/sms?page=${page - 1}&size=${size}&sort=createDate,desc`,
        { headers },
      );
      if (response.ok) {
        const data: PageResponse = await response.json();
        setMessages(data.content);
        setPagination((prev) => ({
          ...prev,
          current: data.number + 1,
          pageSize: data.size,
          total: data.totalElements,
        }));
      }
    } catch {
      // silently fail — table will just be empty
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMessages();
  }, [fetchMessages]);

  const sendSubscriptionFrame = useCallback((action: "subscribe" | "unsubscribe", messageId: number) => {
    const socket = socketRef.current;
    if (socket?.readyState !== WebSocket.OPEN) {
      return;
    }

    socket.send(JSON.stringify({ action, messageId }));
  }, []);

  const syncSubscriptions = useCallback((messageIds: number[]) => {
    const previousIds = subscribedIdsRef.current;
    const nextIds = new Set(messageIds);
    subscribedIdsRef.current = nextIds;

    previousIds.forEach((messageId) => {
      if (!nextIds.has(messageId)) {
        sendSubscriptionFrame("unsubscribe", messageId);
      }
    });

    nextIds.forEach((messageId) => {
      if (!previousIds.has(messageId)) {
        sendSubscriptionFrame("subscribe", messageId);
      }
    });
  }, [sendSubscriptionFrame]);

  useEffect(() => {
    if (isMock) {
      return undefined;
    }

    let disposed = false;

    const connect = async () => {
      const accessToken = await getAccessToken();
      if (disposed || !accessToken) {
        return;
      }

      const socket = new WebSocket(buildSmsStatusWebSocketUrl(accessToken));
      socketRef.current = socket;

      socket.onopen = () => {
        subscribedIdsRef.current.forEach((messageId) => {
          socket.send(JSON.stringify({ action: "subscribe", messageId }));
        });
      };

      socket.onmessage = (event) => {
        const payload = JSON.parse(event.data) as SmsStatusSocketEvent | SmsSocketControlEvent;
        if (payload.type !== "message_status" || !("message" in payload)) {
          return;
        }

        setMessages((current) =>
          upsertMessage(current, payload.message, paginationRef.current.pageSize ?? 10),
        );
      };

      socket.onerror = () => {
        socket.close();
      };

      socket.onclose = () => {
        if (socketRef.current === socket) {
          socketRef.current = null;
        }

        if (!disposed) {
          reconnectTimerRef.current = window.setTimeout(connect, 2000);
        }
      };
    };

    void connect();

    return () => {
      disposed = true;

      if (reconnectTimerRef.current !== null) {
        window.clearTimeout(reconnectTimerRef.current);
      }

      const socket = socketRef.current;
      socketRef.current = null;
      socket?.close();
    };
  }, []);

  useEffect(() => {
    syncSubscriptions(messages.map((message) => message.id));
  }, [messages, syncSubscriptions]);

  const handleTableChange = (pag: TablePaginationConfig) => {
    fetchMessages(pag.current, pag.pageSize);
  };

  const fillPhoneNumber = useCallback(
    (phoneNumber: string) => {
      form.setFieldsValue({ phoneNumber });
      window.scrollTo({ top: 0, behavior: "smooth" });
    },
    [form],
  );

  const checkPortability = useCallback(
    (phoneNumber: string) => {
      navigate(`/portability?phoneNumber=${encodeURIComponent(phoneNumber)}`);
    },
    [navigate],
  );

  const columns = useMemo<ColumnsType<MessageRecord>>(
    () => [
      {
        title: "Phone Number",
        dataIndex: "phoneNumber",
        width: 160,
        render: (value: string) => (
          <Button
            type="link"
            size="small"
            style={{ padding: 0, height: "auto" }}
            onClick={() => fillPhoneNumber(value)}
          >
            {value}
          </Button>
        ),
      },
      {
        title: "Message",
        dataIndex: "content",
        ellipsis: { showTitle: false },
        responsive: ["sm"],
        render: (text: string) => (
          <Tooltip placement="topLeft" title={text}>
            {text}
          </Tooltip>
        ),
      },
      {
        title: "Status",
        dataIndex: "status",
        width: 110,
        render: (status: string) => {
          const config = STATUS_CONFIG[status] || STATUS_CONFIG.UNKNOWN;
          return <Tag color={config.color}>{config.label}</Tag>;
        },
      },
      {
        title: "Sent At",
        dataIndex: "createdAt",
        width: 180,
        responsive: ["md"],
        render: (value: string) =>
          new Date(value).toLocaleString(undefined, {
            dateStyle: "medium",
            timeStyle: "short",
          }),
      },
      {
        title: "Portability",
        key: "portability",
        width: 130,
        align: "center",
        render: (_, record) => (
          <Tooltip title="Check number portability">
            <Button
              size="small"
              icon={<SwapOutlined />}
              onClick={() => checkPortability(record.phoneNumber)}
            >
              Check
            </Button>
          </Tooltip>
        ),
      },
    ],
    [fillPhoneNumber, checkPortability],
  );

  const handleSend = async (values: { phoneNumber: string; message: string }) => {
    setSending(true);
    setResult(null);

    try {
      const headers = await getAuthHeaders();

      const response = await fetch(`${API_URL}/api/v1/sms`, {
        method: "POST",
        headers,
        body: JSON.stringify(values),
      });

      if (response.ok) {
        const createdMessage: MessageRecord = await response.json();
        const pageSize = pagination.pageSize ?? 10;

        setResult({ type: "success", message: "SMS sent successfully" });
        form.resetFields(["message"]);
        setPagination((current) => ({
          ...current,
          current: 1,
          total: (current.total ?? 0) + 1,
        }));
        setMessages((current) => upsertMessage(current, createdMessage, pageSize));
        sendSubscriptionFrame("subscribe", createdMessage.id);
      } else {
        const text = await response.text();
        setResult({ type: "error", message: text || `Error ${response.status}` });
      }
    } catch (err: any) {
      setResult({ type: "error", message: err.message || "Failed to send SMS" });
    } finally {
      setSending(false);
    }
  };

  return (
    <>
      <div style={{ maxWidth: 600, margin: "0 auto" }}>
        <Title level={3}>Send SMS</Title>

        <Card>
          <Form form={form} layout="vertical" onFinish={handleSend}>
            <Form.Item
              label="Phone Number"
              name="phoneNumber"
              rules={[
                { required: true, message: "Phone number is required" },
                {
                  pattern: /^\+[1-9]\d{7,14}$/,
                  message: "Must be in E.164 format (e.g., +40712345678)",
                },
              ]}
            >
              <Input placeholder="+40712345678" />
            </Form.Item>

            <Form.Item
              label="Message"
              name="message"
              rules={[
                { required: true, message: "Message is required" },
                { max: 500, message: "Maximum 500 characters" },
              ]}
            >
              <TextArea rows={4} placeholder="Type your message..." showCount maxLength={500} />
            </Form.Item>

            {result && (
              <Form.Item>
                <Alert type={result.type} message={result.message} showIcon closable />
              </Form.Item>
            )}

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                icon={<SendOutlined />}
                loading={sending}
                block
              >
                Send SMS
              </Button>
            </Form.Item>
          </Form>
        </Card>
      </div>

      <div style={{ marginTop: 32 }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            gap: 12,
            flexWrap: "wrap",
            marginBottom: 16,
          }}
        >
          <Title level={4} style={{ margin: 0 }}>Message History</Title>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => fetchMessages(pagination.current, pagination.pageSize)}
            loading={loading}
          >
            Refresh
          </Button>
        </div>

        <Table<MessageRecord>
          dataSource={messages}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ ...pagination, responsive: true }}
          onChange={handleTableChange}
          size="middle"
          scroll={{ x: "max-content" }}
          expandable={
            isMobile
              ? {
                  expandedRowRender: (record) => (
                    <div style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
                      <strong>Message:</strong> {record.content}
                      <br />
                      <strong>Sent at:</strong>{" "}
                      {new Date(record.createdAt).toLocaleString()}
                    </div>
                  ),
                  rowExpandable: () => true,
                  expandRowByClick: false,
                }
              : undefined
          }
        />
      </div>
    </>
  );
};
