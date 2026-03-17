import type { Metadata } from "next";
import "./tokens.css";
import "./globals.css";
import Navigation from "@/components/Navigation";
import { OrderProvider } from "@/context/OrderContext";

export const metadata: Metadata = {
  title: "Pizza Vibe",
  description: "Order delicious pizzas",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>
        {/* <Navigation /> */}
        <OrderProvider>
          {children}
        </OrderProvider>
      </body>
    </html>
  );
}
