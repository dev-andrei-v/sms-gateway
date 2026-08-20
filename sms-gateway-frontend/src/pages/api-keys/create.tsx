import { useCreate } from "@refinedev/core";
import { Button, DatePicker, Form, Input, Alert, Typography, Space } from "antd";
import { useState } from "react";

const { Text } = Typography;

interface Props {
  onSuccess: () => void;
}

export const ApiKeyCreate: React.FC<Props> = ({ onSuccess }) => {
  const [form] = Form.useForm();
  const { mutate: create, mutation: { isPending: isLoading } } = useCreate();
  const [createdKey, setCreatedKey] = useState<string | null>(null);

  const handleSubmit = (values: { name: string; expiresAt?: any }) => {
    create(
      {
        resource: "api-keys",
        values: {
          name: values.name,
          expiresAt: values.expiresAt?.toISOString() ?? null,
        },
      },
      {
        onSuccess: (data) => {
          setCreatedKey(data.data.key);
        },
      }
    );
  };

  if (createdKey) {
    return (
      <Space direction="vertical" size="middle" style={{ width: "100%" }}>
        <Alert
          type="warning"
          showIcon
          message="Save your API key now!"
          description="This key will only be shown once. Copy it and store it securely."
        />
        <Input.TextArea
          value={createdKey}
          readOnly
          autoSize
          style={{ fontFamily: "monospace" }}
        />
        <Button
          type="primary"
          onClick={() => {
            navigator.clipboard.writeText(createdKey);
          }}
        >
          Copy to Clipboard
        </Button>
        <Button onClick={onSuccess}>Done</Button>
      </Space>
    );
  }

  return (
    <Form form={form} layout="vertical" onFinish={handleSubmit}>
      <Form.Item
        label="Name"
        name="name"
        rules={[
          { required: true, message: "Name is required" },
          { max: 100, message: "Maximum 100 characters" },
        ]}
      >
        <Input placeholder="e.g., Production Key" />
      </Form.Item>

      <Form.Item label="Expires At (optional)" name="expiresAt">
        <DatePicker showTime style={{ width: "100%" }} />
      </Form.Item>

      <Form.Item>
        <Button type="primary" htmlType="submit" loading={isLoading} block>
          Create
        </Button>
      </Form.Item>
    </Form>
  );
};
