import { Edit, useForm } from "@refinedev/antd";
import { Form, Input, InputNumber, Select, Switch } from "antd";

const QUOTA_TYPES = [
  { label: "Unlimited", value: "UNLIMITED" },
  { label: "Per Day", value: "PER_DAY" },
  { label: "Per Month", value: "PER_MONTH" },
  { label: "Total", value: "TOTAL" },
];

export const PlanEdit: React.FC = () => {
  const { formProps, saveButtonProps } = useForm({ resource: "plans" });

  return (
    <Edit saveButtonProps={saveButtonProps}>
      <Form {...formProps} layout="vertical">
        <Form.Item label="Code" name="code" rules={[{ required: true }, { max: 50 }]}>
          <Input />
        </Form.Item>
        <Form.Item label="Name" name="name" rules={[{ required: true }, { max: 100 }]}>
          <Input />
        </Form.Item>
        <Form.Item label="Quota Type" name="quotaType">
          <Select options={QUOTA_TYPES} />
        </Form.Item>
        <Form.Item label="Quota Limit" name="quotaLimit">
          <InputNumber min={1} style={{ width: "100%" }} />
        </Form.Item>
        <Form.Item label="Min Delay (seconds)" name="minDelaySeconds">
          <InputNumber min={0} style={{ width: "100%" }} />
        </Form.Item>
        <Form.Item label="Max Recipients Per Request" name="maxRecipientsPerRequest">
          <InputNumber min={1} style={{ width: "100%" }} />
        </Form.Item>
        <Form.Item label="Active" name="isActive" valuePropName="checked">
          <Switch />
        </Form.Item>
      </Form>
    </Edit>
  );
};
