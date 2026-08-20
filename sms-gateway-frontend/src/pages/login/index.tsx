import { useLogin } from "@refinedev/core";
import { Button, Card, Layout, Space, Tag, Typography } from "antd";
import { LoginOutlined } from "@ant-design/icons";

const { Title, Text } = Typography;
const isMock = import.meta.env.VITE_MOCK === "true";

export const LoginPage: React.FC = () => {
  const { mutate: login } = useLogin();

  return (
    <Layout
      style={{
        height: "100vh",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
      }}
    >
      <Card
        style={{
          width: 400,
          textAlign: "center",
          borderRadius: 12,
          boxShadow: "0 8px 32px rgba(0,0,0,0.15)",
        }}
      >
        <Space direction="vertical" size="large" style={{ width: "100%" }}>
          <div>
            <Title level={2} style={{ marginBottom: 4 }}>
              SMS Gateway
            </Title>
            <Text type="secondary">Sign in to manage your SMS</Text>
            {isMock && (
              <div style={{ marginTop: 8 }}>
                <Tag color="orange">DEV MODE</Tag>
              </div>
            )}
          </div>
          <Button
            type="primary"
            size="large"
            icon={<LoginOutlined />}
            onClick={() => login({})}
            block
          >
            {isMock ? "Sign in as Dev Admin" : "Sign in with Identity Provider"}
          </Button>
        </Space>
      </Card>
    </Layout>
  );
};
