import { List } from "@refinedev/antd";
import { useList, useCustomMutation } from "@refinedev/core";
import {
  Button,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  Popconfirm,
  Form,
} from "antd";
import { useState } from "react";
import { API_URL } from "../../providers/constants";
import { userManager } from "../../providers/auth-provider";

const { Text } = Typography;

interface User {
  id: number;
  externalProviderId: string;
  username: string;
  enabled: boolean;
  planCode: string | null;
  createdAt: string;
  updatedAt: string | null;
}

interface Plan {
  id: number;
  code: string;
  name: string;
}

async function apiCall(path: string, method: string, body?: any) {
  const user = await userManager.getUser();
  const res = await fetch(`${API_URL}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${user?.access_token}`,
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) throw new Error(await res.text());
  if (res.status === 204) return null;
  return res.json();
}

export const UserList: React.FC = () => {
  const { query } = useList<User>({ resource: "users" });
  const { data, isLoading, refetch } = query;
  const { query: plansQuery } = useList<Plan>({ resource: "plans" });
  const plansData = plansQuery.data;
  const [assignModal, setAssignModal] = useState<User | null>(null);
  const [selectedPlan, setSelectedPlan] = useState<string>("");

  const toggleEnabled = async (user: User) => {
    await apiCall(`/api/v1/admin/users/${user.id}/enabled`, "PUT", {
      enabled: !user.enabled,
    });
    refetch();
  };

  const assignPlan = async () => {
    if (!assignModal || !selectedPlan) return;
    await apiCall(`/api/v1/admin/users/${assignModal.id}/plan`, "PUT", {
      planCode: selectedPlan,
    });
    setAssignModal(null);
    setSelectedPlan("");
    refetch();
  };

  const removePlan = async (user: User) => {
    await apiCall(`/api/v1/admin/users/${user.id}/plan`, "DELETE");
    refetch();
  };

  return (
    <>
      <List canCreate={false}>
        <Table dataSource={data?.data} rowKey="id" loading={isLoading} pagination={false}>
          <Table.Column dataIndex="id" title="ID" width={60} />
          <Table.Column dataIndex="username" title="Username" />
          <Table.Column
            dataIndex="enabled"
            title="Status"
            render={(v: boolean) => (
              <Tag color={v ? "green" : "red"}>{v ? "Enabled" : "Disabled"}</Tag>
            )}
          />
          <Table.Column
            dataIndex="planCode"
            title="Plan"
            render={(v: string | null) => (v ? <Tag color="blue">{v}</Tag> : <Text type="secondary">None</Text>)}
          />
          <Table.Column
            dataIndex="createdAt"
            title="Created"
            render={(v: string) => new Date(v).toLocaleDateString()}
          />
          <Table.Column
            title="Actions"
            render={(_: any, record: User) => (
              <Space>
                <Button
                  size="small"
                  onClick={() => toggleEnabled(record)}
                  danger={record.enabled}
                >
                  {record.enabled ? "Disable" : "Enable"}
                </Button>
                <Button size="small" onClick={() => { setAssignModal(record); setSelectedPlan(record.planCode || ""); }}>
                  Assign Plan
                </Button>
                {record.planCode && (
                  <Popconfirm title="Remove plan from this user?" onConfirm={() => removePlan(record)}>
                    <Button size="small" danger>
                      Remove Plan
                    </Button>
                  </Popconfirm>
                )}
              </Space>
            )}
          />
        </Table>
      </List>

      <Modal
        title={`Assign Plan to ${assignModal?.username}`}
        open={!!assignModal}
        onOk={assignPlan}
        onCancel={() => setAssignModal(null)}
        okButtonProps={{ disabled: !selectedPlan }}
      >
        <Form layout="vertical">
          <Form.Item label="Plan">
            <Select
              value={selectedPlan || undefined}
              onChange={setSelectedPlan}
              placeholder="Select a plan"
              options={plansData?.data?.map((p: Plan) => ({
                label: `${p.name} (${p.code})`,
                value: p.code,
              }))}
            />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};
