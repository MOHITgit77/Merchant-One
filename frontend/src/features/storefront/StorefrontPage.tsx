import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { storefrontApi } from '../../api/endpoints';
import Logo from '../../components/brand/Logo';
import './Storefront.css';

export default function StorefrontPage() {
  const { slug } = useParams<{ slug: string }>();
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [productPage, setProductPage] = useState(0);

  const { data: storeRes, isLoading, error } = useQuery({
    queryKey: ['storefront', slug],
    queryFn: () => storefrontApi.getStore(slug!),
    enabled: !!slug,
  });

  const { data: catsRes } = useQuery({
    queryKey: ['storefront-cats', slug],
    queryFn: () => storefrontApi.getCategories(slug!),
    enabled: !!slug,
  });

  const { data: productsRes } = useQuery({
    queryKey: ['storefront-products', slug, selectedCategory, productPage],
    queryFn: () => storefrontApi.getProducts(slug!, { categoryId: selectedCategory || undefined, page: productPage, size: 20 }),
    enabled: !!slug,
  });

  const store = storeRes?.data?.data;
  const categories = catsRes?.data?.data || [];
  const products = productsRes?.data?.data || [];
  const totalPages = productsRes?.data?.totalPages || 0;

  if (isLoading) return (
    <div className="sf-loading"><div className="spinner spinner-lg" /></div>
  );

  if (error || !store) return (
    <div className="sf-error">
      <h1 className="heading-2">Store not found</h1>
      <p className="body-sm">This store doesn't exist or isn't published yet.</p>
    </div>
  );

  return (
    <div className="sf">
      <header className="sf-header">
        {store.coverImageUrl && <div className="sf-cover" style={{ backgroundImage: `url(${store.coverImageUrl})` }} />}
        <div className="sf-header-content">
          {store.logoUrl && <img src={store.logoUrl} alt={store.name} className="sf-logo" />}
          <div className="sf-store-info-box">
            <h1 className="sf-store-name">{store.name}</h1>
            {store.description && <p className="sf-store-desc">{store.description}</p>}
            
            <div className="sf-store-meta">
              {store.address && <span className="meta-item">📍 {store.address}</span>}
              {store.phone && <span className="meta-item">📞 {store.phone}</span>}
              {store.email && <span className="meta-item">✉️ {store.email}</span>}
              {store.pickupEnabled && <span className="meta-item badge badge-primary" style={{marginLeft: '8px', fontSize: '11px', padding: '2px 6px'}}>Pickup Available</span>}
            </div>
          </div>
        </div>
      </header>

      {categories.length > 0 && (
        <nav className="sf-categories" aria-label="Product categories">
          <button className={`sf-cat-btn ${!selectedCategory ? 'sf-cat-active' : ''}`} onClick={() => { setSelectedCategory(''); setProductPage(0); }}>All</button>
          {categories.map((c: any) => (
            <button key={c.id} className={`sf-cat-btn ${selectedCategory === c.id ? 'sf-cat-active' : ''}`} onClick={() => { setSelectedCategory(c.id); setProductPage(0); }}>{c.name}</button>
          ))}
        </nav>
      )}

      <main className="sf-products">
        {products.length === 0 ? (
          <div className="sf-empty">No products available{selectedCategory ? ' in this category' : ''}.</div>
        ) : (
          <div className="sf-grid">
            {products.map((p: any) => (
              <div key={p.id} className="sf-product-card">
                <div className="sf-product-img">
                  {p.images && p.images.length > 0 ? <img src={p.images[0].imageUrl} alt={p.name} /> : <div className="sf-product-placeholder">📦</div>}
                </div>
                <div className="sf-product-body">
                  <h3 className="sf-product-name">{p.name}</h3>
                  {p.description && <p className="sf-product-desc">{p.description}</p>}
                  <div className="sf-product-footer">
                    <div className="sf-product-price">
                      ₹{Number(p.price).toLocaleString('en-IN')}
                      {p.compareAtPrice && Number(p.compareAtPrice) > Number(p.price) && (
                        <span className="sf-product-compare">₹{Number(p.compareAtPrice).toLocaleString('en-IN')}</span>
                      )}
                    </div>
                    <span className="sf-product-unit">per {p.unit?.toLowerCase()}</span>
                  </div>
                  {p.totalStock <= 0 && <div className="sf-out-of-stock">Out of Stock</div>}
                </div>
              </div>
            ))}
          </div>
        )}

        {totalPages > 1 && (
          <div className="pagination" style={{ marginTop: 'var(--space-6)' }}>
            <button className="btn btn-secondary btn-sm" disabled={productPage === 0} onClick={() => setProductPage(p => p - 1)}>Previous</button>
            <span className="pagination-info">Page {productPage + 1} of {totalPages}</span>
            <button className="btn btn-secondary btn-sm" disabled={productPage >= totalPages - 1} onClick={() => setProductPage(p => p + 1)}>Next</button>
          </div>
        )}
      </main>

      <footer className="sf-footer">
        <p>Powered by <Logo size="sm" /></p>
      </footer>
    </div>
  );
}
