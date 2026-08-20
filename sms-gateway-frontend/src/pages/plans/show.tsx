import { Show } from "@refinedev/antd";
import { useShow } from "@refinedev/core";
import { Descriptions, Tag } from "antd";

export const PlanShow: React.FC = () => {
  const { query } = useShow({ resource: "plans" });
  const record = query?.data?.data;

  return (
    <Show>
      <Descriptions bordered column={1}>
        <Descriptions.Item label="ID">{record?.id}</Descriptions.Item>
        <Descriptions.Item label="Code">{record?.code}</Descriptions.Item>
        <Descriptions.Item label="Name">{record?.name}</Descriptions.Item>
        <Descriptions.Item label="Quota Type">
          <Tag>{record?.quotaType}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Quota Limit">
          {record?.quotaLimit ?? "N/A"}
        </Descriptions.Item>
        <Descriptions.Item label="Min Delay (seconds)">
          {record?.minDelaySeconds ?? "N/A"}
        </Descriptions.Item>
        <Descriptions.Item label="Max Recipients Per Request">
          {record?.maxRecipientsPerRequest ?? "N/A"}
        </Descriptions.Item>
        <Descriptions.Item label="Status">
          <Tag color={record?.isActive ? "green" : "red"}>
            {record?.isActive ? "Active" : "Inactive"}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Created">
          {record?.createdAt && new Date(record.createdAt).toLocaleString()}
        </Descriptions.Item>
        <Descriptions.Item label="Updated">
          {record?.updatedAt ? new Date(record.updatedAt).toLocaleString() : "Never"}
        </Descriptions.Item>
      </Descriptions>
    </Show>
  );
};
