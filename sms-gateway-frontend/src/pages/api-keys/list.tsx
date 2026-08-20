import { useList, useDelete } from "@refinedev/core";
import {
  Button,
  Card,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
  Popconfirm,
} from "antd";
import { DeleteOutlined, PlusOutlined } from "@ant-design/icons";
import { useState } from "react";
import { ApiKeyCreate } from "./create";
import type { ColumnsType } from "antd/es/table";

const { Title, Text } = Typography;

interface ApiKey {
  id: number;
  name: string;
  prefix: string;
  enabled: boolean;
  expiresAt: string | null;
  lastUsedAt: string | null;
  createdAt: string;
}

export const ApiKeyList: React.FC = () => {
  const [createOpen, setCreateOpen] = useState(false);
  const { query } = useList<ApiKey>({ resource: "api-keys" });
  const { data, isLoading, refetch } = query;
  const { mutate: deleteKey } = useDelete();

  const columns: ColumnsType<ApiKey> = [
    {
      title: "Name",
      dataIndex: "name",
      key: "name",
    },
    {
      title: "Prefix",
      dataIndex: "prefix",
      key: "prefix",
      render: (prefix: string) => <Text code>{prefix}...</Text>,
    },
    {
      title: "Status",
      dataIndex: "enabled",
      key: "enabled",
      render: (enabled: boolean) => (
        <Tag color={enabled ? "green" : "red"}>{enabled ? "Active" : "Revoked"}</Tag>
      ),
    },
    {
      title: "Expires",
      dataIndex: "expiresAt",
      key: "expiresAt",
      render: (v: string | null) => (v ? new Date(v).toLocaleDateString() : "Never"),
    },
    {
      title: "Last Used",
      dataIndex: "lastUsedAt",
      key: "lastUsedAt",
      render: (v: string | null) => (v ? new Date(v).toLocaleString() : "Never"),
    },
    {
      title: "Created",
      dataIndex: "createdAt",
      key: "createdAt",
      render: (v: string) => new Date(v).toLocaleDateString(),
    },
    {
      title: "Actions",
      key: "actions",
      render: (_: any, record: ApiKey) => (
        <Popconfirm
          title="Revoke this API key?"
          description="This action cannot be undone."
          onConfirm={() =>
            deleteKey(
              { resource: "api-keys", id: record.id },
              { onSuccess: () => refetch() }
            )
          }
        >
          <Button type="text" danger icon={<DeleteOutlined />} disabled={!record.enabled} />
        </Popconfirm>
      ),
    },
  ];

  return (
    <>
      <Space style={{ display: "flex", justifyContent: "space-between", marginBottom: 16 }}>
        <Title level={3} style={{ margin: 0 }}>
          API Keys
        </Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
          Create API Key
        </Button>
      </Space>

      <Table
        dataSource={data?.data}
        columns={columns}
        rowKey="id"
        loading={isLoading}
        pagination={false}
      />

      <Modal
        title="Create API Key"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        footer={null}
        destroyOnClose
      >
        <ApiKeyCreate
          onSuccess={() => {
            setCreateOpen(false);
            refetch();
          }}
        />
      </Modal>
    </>
  );
};
