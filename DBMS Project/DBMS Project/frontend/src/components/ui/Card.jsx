import './Card.css';

export const Card = ({ 
  children, 
  className = '', 
  hoverable = true,
  gradient = false,
  neon = false,
  ...props 
}) => {
  const classes = [
    'card',
    hoverable && 'card-hoverable',
    gradient && 'card-gradient',
    neon && 'card-neon',
    className
  ].filter(Boolean).join(' ');

  return <div className={classes} {...props}>{children}</div>;
};

export const CardHeader = ({ children, className = '' }) => (
  <div className={`card-header ${className}`}>{children}</div>
);

export const CardBody = ({ children, className = '' }) => (
  <div className={`card-body ${className}`}>{children}</div>
);

export const CardFooter = ({ children, className = '' }) => (
  <div className={`card-footer ${className}`}>{children}</div>
);

export const StatCard = ({ 
  icon, 
  label, 
  value, 
  trend,
  trendLabel,
  color = 'accent'
}) => (
  <Card hoverable gradient className="stat-card">
    <CardBody>
      <div className="stat-icon" style={{ color: `var(--${color})` }}>
        {icon}
      </div>
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {trend && (
        <div className={`stat-trend ${trend > 0 ? 'positive' : 'negative'}`}>
          <span className="trend-icon">{trend > 0 ? '↑' : '↓'}</span>
          <span className="trend-value">{Math.abs(trend)}%</span>
          <span className="trend-label">{trendLabel}</span>
        </div>
      )}
    </CardBody>
  </Card>
);
