-- Smart Clothing Sales & Inventory Management System
-- PostgreSQL Database Schema

-- Drop tables if they exist (ordered to respect foreign key constraints)
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS products CASCADE;

-- 1. Products Table (Clothing Items Inventory)
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL, -- e.g., Shirts, Pants, Hoodies, Accessories
    size VARCHAR(10) NOT NULL,     -- e.g., S, M, L, XL, XXL
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    stock_quantity INT NOT NULL CHECK (stock_quantity >= 0),
    min_stock_level INT NOT NULL DEFAULT 5 CHECK (min_stock_level >= 0)
);

-- 2. Customers Table
CREATE TABLE customers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    address TEXT
);

-- 3. Orders Table
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    customer_id INT REFERENCES customers(id) ON DELETE SET NULL,
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 CHECK (total_amount >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'Pending' CHECK (status IN ('Pending', 'Completed', 'Cancelled'))
);

-- 4. Order Items Table (Relationship between Orders and Products)
CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id INT REFERENCES orders(id) ON DELETE CASCADE,
    product_id INT REFERENCES products(id) ON DELETE SET NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL CHECK (unit_price >= 0)
);

-- 5. Payments Table
CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    order_id INT REFERENCES orders(id) ON DELETE CASCADE,
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount >= 0),
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('Cash', 'Card', 'UPI')),
    status VARCHAR(20) NOT NULL DEFAULT 'Completed' CHECK (status IN ('Completed', 'Pending', 'Failed'))
);

-- Insert Sample Products
INSERT INTO products (name, category, size, price, stock_quantity, min_stock_level) VALUES
('Classic Denim Jacket', 'Outerwear', 'M', 79.99, 15, 4),
('Classic Denim Jacket', 'Outerwear', 'L', 79.99, 3, 4), -- Under-stock example
('Slim Fit Chinos', 'Pants', '32', 49.99, 25, 6),
('Oversized Cotton Hoodie', 'Hoodies', 'L', 59.99, 18, 5),
('Oversized Cotton Hoodie', 'Hoodies', 'XL', 59.99, 2, 5), -- Under-stock example
('Graphic Tee - Retro Space', 'Shirts', 'M', 24.99, 40, 10),
('Graphic Tee - Retro Space', 'Shirts', 'L', 24.99, 35, 10),
('Plaid Flannel Shirt', 'Shirts', 'S', 34.99, 12, 5),
('Plaid Flannel Shirt', 'Shirts', 'M', 34.99, 8, 5),
('Athletic Fleece Joggers', 'Pants', 'M', 45.00, 30, 8),
('Leather Chelsea Boots', 'Shoes', '10', 120.00, 8, 3),
('Crewneck Knit Sweater', 'Sweaters', 'L', 65.00, 0, 5); -- Out-of-stock example

-- Insert Sample Customers
INSERT INTO customers (name, email, phone, address) VALUES
('John Doe', 'john.doe@example.com', '555-0199', '123 Elm St, Springfield'),
('Jane Smith', 'jane.smith@example.com', '555-0142', '456 Oak Ave, Metropolis'),
('Alex Rodriguez', 'alex.rod@example.com', '555-0188', '789 Pine Rd, Gotham'),
('Emily Chen', 'emily.chen@example.com', '555-0155', '321 Maple Ln, Coast City');

-- Insert Sample Orders
-- Order 1: Completed order for John Doe
INSERT INTO orders (customer_id, order_date, total_amount, status) VALUES
(1, CURRENT_DATE - INTERVAL '10 days', 214.97, 'Completed');

INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES
(1, 1, 1, 79.99),  -- Denim Jacket (M)
(1, 3, 2, 49.99),  -- Chinos (32)
(1, 6, 1, 24.99);  -- Graphic Tee (M)

INSERT INTO payments (order_id, payment_date, amount, payment_method, status) VALUES
(1, CURRENT_DATE - INTERVAL '10 days', 214.97, 'Card', 'Completed');

-- Order 2: Completed order for Jane Smith
INSERT INTO orders (customer_id, order_date, total_amount, status) VALUES
(2, CURRENT_DATE - INTERVAL '5 days', 119.98, 'Completed');

INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES
(2, 4, 2, 59.99);  -- Oversized Hoodie (L)

INSERT INTO payments (order_id, payment_date, amount, payment_method, status) VALUES
(2, CURRENT_DATE - INTERVAL '5 days', 119.98, 'UPI', 'Completed');

-- Order 3: Pending order for Alex Rodriguez
INSERT INTO orders (customer_id, order_date, total_amount, status) VALUES
(3, CURRENT_DATE - INTERVAL '2 days', 169.99, 'Pending');

INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES
(3, 3, 1, 49.99),  -- Chinos (32)
(3, 11, 1, 120.00); -- Chelsea Boots (10)

-- Order 4: Completed order for Emily Chen
INSERT INTO orders (customer_id, order_date, total_amount, status) VALUES
(4, CURRENT_DATE - INTERVAL '1 day', 69.98, 'Completed');

INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES
(4, 8, 2, 34.99);  -- Flannel Shirt (S)

INSERT INTO payments (order_id, payment_date, amount, payment_method, status) VALUES
(4, CURRENT_DATE - INTERVAL '1 day', 69.98, 'Cash', 'Completed');
