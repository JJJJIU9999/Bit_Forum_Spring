function PageHeader({ title, description, badge }) {
  return (
    <div className="section-heading">
      <div>
        <h2>{title}</h2>
        {description && <p>{description}</p>}
      </div>
      {badge !== undefined && badge !== null && <span>{badge}</span>}
    </div>
  )
}

export default PageHeader
