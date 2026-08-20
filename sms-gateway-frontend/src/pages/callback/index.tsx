import { useEffect } from "react";
import { useNavigate } from "react-router";
import { Spin, Layout } from "antd";
import { userManager } from "../../providers/auth-provider";

export const CallbackPage: React.FC = () => {
  const navigate = useNavigate();

  useEffect(() => {
    userManager
      .signinRedirectCallback()
      .then(() => {
        navigate("/", { replace: true });
      })
      .catch((err) => {
        console.error("OIDC callback error:", err);
        navigate("/login", { replace: true });
      });
  }, [navigate]);

  return (
    <Layout
      style={{
        height: "100vh",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
      }}
    >
      <Spin size="large" tip="Signing in..." />
    </Layout>
  );
};
