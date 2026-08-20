import {
  Alert,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  Tag,
  Typography,
} from "antd";
import { SearchOutlined } from "@ant-design/icons";
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router";
import { API_URL } from "../../providers/constants";

const { Title, Text, Link } = Typography;
const isMock = import.meta.env.VITE_MOCK === "true";

interface PortabilityResponse {
  number: string;
  queryUrl: string;
  sourceLanguage: string;
  fetchedAt: string;
  status: string;
  ported: boolean | null;
  title: string | null;
  operators: {
    current: string | null;
    initial: string | null;
  };
  timestamps: {
    current: string | null;
    currentIso: string | null;
    infoValidOn: string | null;
    infoValidOnIso: string | null;
  };
  numberType: string | null;
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

const STATUS_TAG: Record<string, { color: string; label: string }> = {
  ported: { color: "success", label: "Ported" },
  not_ported: { color: "default", label: "Not ported" },
  unknown: { color: "warning", label: "Unknown" },
};

function formatStatus(status: string): { color: string; label: string } {
  return STATUS_TAG[status] || { color: "default", label: status };
}

export const PortabilityCheck: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<PortabilityResponse | null>(null);
  const [searchParams, setSearchParams] = useSearchParams();

  const lookup = async (values: { phoneNumber: string }) => {
    const phoneNumber = values.phoneNumber.trim();
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const headers = await getAuthHeaders();
      const response = await fetch(
        `${API_URL}/api/v1/portability/${encodeURIComponent(phoneNumber)}`,
        { headers },
      );

      if (response.ok) {
        const data: PortabilityResponse = await response.json();
        setResult(data);
      } else {
        let message = `Error ${response.status}`;
        try {
          const body = await response.json();
          if (body?.message) message = body.message;
        } catch {
          /* ignore */
        }
        setError(message);
      }
    } catch (err: any) {
      setError(err?.message || "Failed to look up portability");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const phoneNumber = searchParams.get("phoneNumber");
    if (phoneNumber) {
      form.setFieldsValue({ phoneNumber });
      void lookup({ phoneNumber });
      // clear the param so refresh doesn't re-fire
      const next = new URLSearchParams(searchParams);
      next.delete("phoneNumber");
      setSearchParams(next, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const status = result ? formatStatus(result.status) : null;

  return (
    <div style={{ maxWidth: 720, margin: "0 auto" }}>
      <Title level={3}>Number Portability</Title>

      <Card>
        <Form form={form} layout="vertical" onFinish={lookup}>
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
            <Input placeholder="+40712345678" autoComplete="off" />
          </Form.Item>

          {error && (
            <Form.Item>
              <Alert type="error" message={error} showIcon closable onClose={() => setError(null)} />
            </Form.Item>
          )}

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              icon={<SearchOutlined />}
              loading={loading}
              block
            >
              Check
            </Button>
          </Form.Item>
        </Form>
      </Card>

      {result && status && (
        <Card style={{ marginTop: 16 }}>
          <div style={{ display: "flex", flexWrap: "wrap", gap: 12, alignItems: "center", marginBottom: 16 }}>
            <Title level={4} style={{ margin: 0 }}>
              {result.number}
            </Title>
            <Tag color={status.color}>{status.label}</Tag>
            {result.numberType && <Tag>{result.numberType}</Tag>}
          </div>

          <Descriptions
            column={{ xs: 1, sm: 1, md: 2 }}
            size="small"
            bordered
            items={[
              {
                key: "current",
                label: "Current operator",
                children: result.operators.current ?? <Text type="secondary">—</Text>,
              },
              {
                key: "initial",
                label: "Initial operator",
                children: result.operators.initial ?? <Text type="secondary">—</Text>,
              },
              {
                key: "ported-at",
                label: "Ported at",
                children:
                  result.timestamps.currentIso ?? result.timestamps.current ?? (
                    <Text type="secondary">—</Text>
                  ),
              },
              {
                key: "valid-on",
                label: "Info valid on",
                children:
                  result.timestamps.infoValidOnIso ?? result.timestamps.infoValidOn ?? (
                    <Text type="secondary">—</Text>
                  ),
              },
              {
                key: "fetched-at",
                label: "Fetched at",
                children: new Date(result.fetchedAt).toLocaleString(),
              },
              {
                key: "source",
                label: "Source",
                children: (
                  <Link href={result.queryUrl} target="_blank" rel="noreferrer">
                    portabilitate.ro
                  </Link>
                ),
              },
            ]}
          />
        </Card>
      )}
    </div>
  );
};
