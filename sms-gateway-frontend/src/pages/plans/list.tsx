import {
  List,
  useTable,
  EditButton,
  ShowButton,
  DeleteButton,
} from "@refinedev/antd";
import { Table, Tag } from "antd";

export const PlanList: React.FC = () => {
  const { tableProps } = useTable({ resource: "plans", syncWithLocation: true });

  return (
    <List>
      <Table {...tableProps} rowKey="id">
        <Table.Column dataIndex="code" title="Code" />
        <Table.Column dataIndex="name" title="Name" />
        <Table.Column
          dataIndex="quotaType"
          title="Quota Type"
          render={(v: string) => <Tag>{v}</Tag>}
        />
        <Table.Column dataIndex="quotaLimit" title="Quota Limit" render={(v) => v ?? "N/A"} />
        <Table.Column
          dataIndex="minDelaySeconds"
          title="Min Delay (s)"
          render={(v) => v ?? "N/A"}
        />
        <Table.Column
          dataIndex="maxRecipientsPerRequest"
          title="Max Recipients"
          render={(v) => v ?? "N/A"}
        />
        <Table.Column
          dataIndex="isActive"
          title="Status"
          render={(v: boolean) => (
            <Tag color={v ? "green" : "red"}>{v ? "Active" : "Inactive"}</Tag>
          )}
        />
        <Table.Column
          title="Actions"
          render={(_, record: any) => (
            <>
              <EditButton hideText size="small" recordItemId={record.id} />
              <ShowButton hideText size="small" recordItemId={record.id} />
              <DeleteButton hideText size="small" recordItemId={record.id} />
            </>
          )}
        />
      </Table>
    </List>
  );
};
