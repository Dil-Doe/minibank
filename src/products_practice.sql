
-- Создание таблицы
CREATE TABLE products (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          name CHAR(255) NOT NULL,
                          price BIGINT NOT NULL,
                          category CHAR(50) NOT NULL,
                          in_stock BOOLEAN NOT NULL,
                          date_of_incoming DATE NOT NULL
);

-- Тестовые данные
INSERT INTO products (name, price, category, in_stock, date_of_incoming)
VALUES
    ('Смартфон', 50000, 'Электроника', true, CURRENT_DATE),
    ('Ноутбук', 100000, 'Электроника', true, CURRENT_DATE),
    ('Наушники', 5000, 'Электроника', false, CURRENT_DATE),
    ('Книга', 1500, 'Книги', true, CURRENT_DATE - INTERVAL '1 year'),
    ('Книга 2', 2000, 'Книги', true, CURRENT_DATE - INTERVAL '8 months'),
    ('Книга 3', 1000, 'Книги', false, CURRENT_DATE - INTERVAL '3 years'),
    ('Стиралка', 15000, 'Бытовая техника', true, CURRENT_DATE);

-- 1. Вывести названия и цены товаров категории «Электроника» в наличии
SELECT name, price
FROM products
WHERE category = 'Электроника'
  AND in_stock = true;

-- 2. Увеличить цену всех товаров категории «Книги» на 10%
UPDATE products
SET price = price * 1.10
WHERE category = 'Книги';

-- 3. Добавить новый товар
INSERT INTO products (name, price, category, in_stock, date_of_incoming)
VALUES ('Книга 4', 500, 'Книги', true, CURRENT_DATE);

-- 4. Удалить все товары, которых нет в наличии
DELETE FROM products
WHERE in_stock = false;

-- 5. Снизить цену книг, поступивших от полугода до двух лет назад, на 20%
-- и добавить к названию «(Скидочная позиция)»
UPDATE products
SET
    price = price * 0.80,
    name = RTRIM(name) || ' (Скидочная позиция)'
WHERE category = 'Книги'
  AND date_of_incoming BETWEEN CURRENT_DATE - INTERVAL '2 years'
  AND CURRENT_DATE - INTERVAL '6 months';

-- Проверка итоговой таблицы
SELECT
    RTRIM(name) AS name,
    price,
    RTRIM(category) AS category,
    in_stock,
    date_of_incoming
FROM products
ORDER BY category, name;
