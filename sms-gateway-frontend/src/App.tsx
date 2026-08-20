import { Authenticated, Refine } from "@refinedev/core";
import { RefineKbar, RefineKbarProvider } from "@refinedev/kbar";

import {
  ErrorComponent,
  ThemedLayout,
  ThemedSider,
  useNotificationProvider,
} from "@refinedev/antd";
import "@refinedev/antd/dist/reset.css";

import routerProvider, {
  CatchAllNavigate,
  DocumentTitleHandler,
  NavigateToResource,
  UnsavedChangesNotifier,
} from "@refinedev/react-router";
import { App as AntdApp } from "antd";
import { BrowserRouter, Outlet, Route, Routes } from "react-router";
import {
  MessageOutlined,
  KeyOutlined,
  AppstoreOutlined,
  TeamOutlined,
  SwapOutlined,
} from "@ant-design/icons";
import { Header } from "./components/header";
import { ColorModeContextProvider } from "./contexts/color-mode";
import { authProvider as oidcAuthProvider } from "./providers/auth-provider";
import { mockAuthProvider } from "./providers/mock-auth-provider";
import { dataProvider } from "./providers/data-provider";
import { accessControlProvider, initAccessControl } from "./providers/access-control";

const isMock = import.meta.env.VITE_MOCK === "true";
const authProvider = isMock ? mockAuthProvider : oidcAuthProvider;
initAccessControl(authProvider.getPermissions as () => Promise<string[]>);

import { LoginPage } from "./pages/login";
import { CallbackPage } from "./pages/callback";
import { SmsSend } from "./pages/sms/send";
import { ApiKeyList } from "./pages/api-keys";
import { PlanList, PlanCreate, PlanEdit, PlanShow } from "./pages/plans";
import { UserList } from "./pages/users";
import { PortabilityCheck } from "./pages/portability";

function App() {
  return (
    <BrowserRouter>
      <RefineKbarProvider>
        <ColorModeContextProvider>
          <AntdApp>
            <Refine
              authProvider={authProvider}
              dataProvider={dataProvider}
              accessControlProvider={accessControlProvider}
              notificationProvider={useNotificationProvider}
              routerProvider={routerProvider}
              resources={[
                {
                  name: "sms",
                  list: "/sms",
                  meta: {
                    label: "SMS",
                    icon: <MessageOutlined />,
                  },
                },
                {
                  name: "portability",
                  list: "/portability",
                  meta: {
                    label: "Portability",
                    icon: <SwapOutlined />,
                  },
                },
                {
                  name: "api-keys",
                  list: "/api-keys",
                  meta: {
                    label: "API Keys",
                    icon: <KeyOutlined />,
                  },
                },
                {
                  name: "plans",
                  list: "/plans",
                  create: "/plans/create",
                  edit: "/plans/edit/:id",
                  show: "/plans/show/:id",
                  meta: {
                    label: "Plans",
                    icon: <AppstoreOutlined />,
                    canDelete: true,
                  },
                },
                {
                  name: "users",
                  list: "/users",
                  meta: {
                    label: "Users",
                    icon: <TeamOutlined />,
                  },
                },
              ]}
              options={{
                syncWithLocation: true,
                warnWhenUnsavedChanges: true,
              }}
            >
              <Routes>
                <Route
                  element={
                    <Authenticated
                      key="auth"
                      fallback={<CatchAllNavigate to="/login" />}
                    >
                      <ThemedLayout
                        Header={() => <Header sticky />}
                        Sider={(props) => <ThemedSider {...props} fixed />}
                        Title={({ collapsed }) => (
                          <div
                            style={{
                              display: "flex",
                              alignItems: "center",
                              gap: 8,
                              padding: collapsed ? "0 8px" : "0 16px",
                            }}
                          >
                            <MessageOutlined style={{ fontSize: 20 }} />
                            {!collapsed && (
                              <span style={{ fontWeight: 600, fontSize: 16 }}>
                                SMS Gateway
                              </span>
                            )}
                          </div>
                        )}
                      >
                        <Outlet />
                      </ThemedLayout>
                    </Authenticated>
                  }
                >
                  <Route
                    index
                    element={<NavigateToResource resource="sms" />}
                  />
                  <Route path="/sms" element={<SmsSend />} />
                  <Route path="/portability" element={<PortabilityCheck />} />
                  <Route path="/api-keys" element={<ApiKeyList />} />
                  <Route path="/plans">
                    <Route index element={<PlanList />} />
                    <Route path="create" element={<PlanCreate />} />
                    <Route path="edit/:id" element={<PlanEdit />} />
                    <Route path="show/:id" element={<PlanShow />} />
                  </Route>
                  <Route path="/users" element={<UserList />} />
                  <Route path="*" element={<ErrorComponent />} />
                </Route>

                <Route
                  element={
                    <Authenticated
                      key="no-auth"
                      fallback={<Outlet />}
                    >
                      <NavigateToResource resource="sms" />
                    </Authenticated>
                  }
                >
                  <Route path="/login" element={<LoginPage />} />
                </Route>

                <Route path="/auth/callback" element={<CallbackPage />} />
              </Routes>

              <RefineKbar />
              <UnsavedChangesNotifier />
              <DocumentTitleHandler
                handler={({ autoGeneratedTitle }) => {
                  const match = autoGeneratedTitle?.match(/^(.+?)\s*\|/);
                  const prefix = match?.[1]?.trim();
                  return prefix ? `${prefix} | SMS Gateway` : "SMS Gateway";
                }}
              />
            </Refine>
          </AntdApp>
        </ColorModeContextProvider>
      </RefineKbarProvider>
    </BrowserRouter>
  );
}

export default App;
